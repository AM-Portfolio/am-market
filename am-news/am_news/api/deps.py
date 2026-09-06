from fastapi import Depends, HTTPException, status
from fastapi.security import HTTPAuthorizationCredentials, HTTPBearer

from am_news.container import Container
from am_news.domain.entities import Principal
from am_news.schemas.news import ErrorEnvelope
from am_news.settings import settings

_bearer = HTTPBearer(auto_error=False)

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
        return Principal(subject="user", roles=("user",), access_token=credentials.credentials)

    async def require_admin(principal: Principal = Depends(require_user)) -> Principal:
        if {"admin", "super_admin"}.intersection(principal.roles):
            return principal
        _forbidden()
        raise AssertionError
