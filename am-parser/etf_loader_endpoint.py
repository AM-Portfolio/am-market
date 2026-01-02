"""
ETF Loader API Endpoint
Add this to am_api/etf_api.py to enable loading ETF data via API
"""

@router.post("/load-from-json")
async def load_etfs_from_json(
    file: UploadFile = File(..., description="ETF details JSON file"),
    dry_run: bool = Query(default=False, description="Validate only, don't persist")
):
    """
    Load ETF data from JSON file
    Accepts etf_details.json and loads all ETFs into database
    """
    try:
        import json
        from am_etf.models import ETFInstrument
        
        # Read and parse JSON
        content = await file.read()
        data = json.loads(content)
        
        if not isinstance(data, list):
            raise HTTPException(
                status_code=status.HTTP_400_BAD_REQUEST,
                detail="Expected a JSON array of ETF records"
            )
        
        # Parse ETF instruments
        instruments = []
        errors = []
        for i, rec in enumerate(data):
            try:
                instruments.append(ETFInstrument(**rec))
            except Exception as e:
                errors.append(f"Record {i}: {str(e)}")
        
        print(f"📊 Parsed {len(instruments)} ETF instruments from {len(data)} records")
        
        if dry_run:
            return {
                "status": "validated",
                "total_records": len(data),
                "valid_instruments": len(instruments),
                "errors": errors[:10] if errors else [],  # First 10 errors
                "message": "Dry run: not persisted to database"
            }
        
        # Save to database
        etf_service = ETFService()
        inserted_count = await etf_service.bulk_upsert(instruments)
        await etf_service.close()
        
        return {
            "status": "success",
            "total_records": len(data),
            "valid_instruments": len(instruments),
            "inserted_count": inserted_count,
            "errors": errors[:10] if errors else [],
            "message": f"Successfully loaded {inserted_count} ETFs into database"
        }
        
    except json.JSONDecodeError as e:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail=f"Invalid JSON: {str(e)}"
        )
    except Exception as e:
        print(f"❌ ETF load error: {e}")
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail=f"Failed to load ETFs: {str(e)}"
        )
