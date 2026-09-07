from urllib.parse import urlparse

import jwt
from fastapi import Depends, HTTPException, status
from fastapi.security import HTTPAuthorizationCredentials, HTTPBearer
from jwt import InvalidTokenError, PyJWKClient, PyJWKClientConnectionError

from am_news.container import Container
from am_news.domain.entities import Principal
from am_news.schemas.news import ErrorEnvelope
from am_news.settings import settings

_bearer = HTTPBearer(auto_error=False)
_JWKS_HEADERS = {
    "User-Agent": "am-news/1.0",
    "Accept": "application/json",
}
_ADMIN_ROLES = frozenset({"admin", "super_admin"})
_jwk_client: PyJWKClient | None = None

try:
    from am_platform_security import require_any_roles, require_auth_context

    if settings.oidc_issuer and settings.oidc_jwks_url:
        _CTX_USER = require_auth_context()
        _CTX_ADMIN = require_any_roles(["admin", "super_admin"])
    else:
        _CTX_USER = None
        _CTX_ADMIN = None
except ImportError:
    _CTX_USER = None
    _CTX_ADMIN = None


def get_container() -> Container:
    from am_news.main import app

    container = getattr(app.state, "container", None)
    if container is None:
        raise RuntimeError("container_not_ready")
    return container


def _unauthorized() -> None:
    raise HTTPException(
        status_code=status.HTTP_401_UNAUTHORIZED,
        detail=ErrorEnvelope(error_code="UNAUTHORIZED", message="JWT required").model_dump(),
    )


def _forbidden() -> None:
    raise HTTPException(
        status_code=status.HTTP_403_FORBIDDEN,
        detail=ErrorEnvelope(error_code="FORBIDDEN", message="admin role required").model_dump(),
    )


def _oidc_unavailable() -> None:
    raise HTTPException(
        status_code=status.HTTP_503_SERVICE_UNAVAILABLE,
        detail=ErrorEnvelope(error_code="OIDC_UNAVAILABLE", message="JWKS unavailable").model_dump(),
    )


def _normalize_issuer(url: str) -> str:
    return url.replace("https://", "http://", 1)


def _issuer_matches(token_issuer: str | None, configured_issuer: str) -> bool:
    if not token_issuer:
        return False
    if token_issuer == configured_issuer:
        return True
    if _normalize_issuer(token_issuer) == _normalize_issuer(configured_issuer):
        return True
    token_path = urlparse(_normalize_issuer(token_issuer)).path.rstrip("/")
    configured_path = urlparse(_normalize_issuer(configured_issuer)).path.rstrip("/")
    return bool(token_path) and token_path == configured_path


def _roles_from_claims(claims: dict) -> tuple[str, ...]:
    roles: list[str] = []
    top = claims.get("roles")
    if isinstance(top, list):
        roles.extend(str(role) for role in top)
    realm = claims.get("realm_access")
    if isinstance(realm, dict):
        realm_roles = realm.get("roles")
        if isinstance(realm_roles, list):
            roles.extend(str(role) for role in realm_roles)
    if not roles:
        return ("user",)
    return tuple(dict.fromkeys(roles))


def _principal_from_claims(claims: dict, token: str) -> Principal:
    subject = claims.get("sub")
    return Principal(
        subject=str(subject) if subject else "user",
        roles=_roles_from_claims(claims),
        access_token=token,
    )


def _jwk() -> PyJWKClient:
    global _jwk_client
    if _jwk_client is None:
        _jwk_client = PyJWKClient(
            settings.oidc_jwks_url,
            headers=_JWKS_HEADERS,
            cache_jwk_set=True,
            lifespan=300,
        )
    return _jwk_client


def _claims_verified(token: str) -> dict:
    try:
        signing_key = _jwk().get_signing_key_from_jwt(token).key
        claims = jwt.decode(
            token,
            signing_key,
            algorithms=["RS256"],
            options={"verify_aud": False, "verify_iss": False},
        )
    except PyJWKClientConnectionError:
        _oidc_unavailable()
        raise AssertionError
    except InvalidTokenError:
        _unauthorized()
        raise AssertionError
    issuer = claims.get("iss")
    if not _issuer_matches(str(issuer) if issuer else None, settings.oidc_issuer):
        _unauthorized()
        raise AssertionError
    return claims


def _claims_unverified(token: str) -> dict:
    try:
        claims = jwt.decode(
            token,
            options={"verify_signature": False, "verify_aud": False, "verify_exp": False},
        )
    except InvalidTokenError:
        return {}
    return claims if isinstance(claims, dict) else {}


if _CTX_USER is not None:

    async def require_user(context=Depends(_CTX_USER)) -> Principal:
        return Principal(
            subject=context.subject,
            roles=tuple(context.roles),
            access_token=getattr(context, "access_token", ""),
        )

    async def require_admin(context=Depends(_CTX_ADMIN)) -> Principal:
        return Principal(
            subject=context.subject,
            roles=tuple(context.roles),
            access_token=getattr(context, "access_token", ""),
        )

else:

    async def require_user(
        credentials: HTTPAuthorizationCredentials | None = Depends(_bearer),
    ) -> Principal:
        if credentials is None or not credentials.credentials:
            _unauthorized()
        token = credentials.credentials
        if settings.oidc_issuer and settings.oidc_jwks_url:
            claims = _claims_verified(token)
        else:
            claims = _claims_unverified(token)
            if not claims:
                return Principal(subject="user", roles=("user",), access_token=token)
        return _principal_from_claims(claims, token)

    async def require_admin(principal: Principal = Depends(require_user)) -> Principal:
        if _ADMIN_ROLES.intersection(principal.roles):
            return principal
        _forbidden()
        raise AssertionError
