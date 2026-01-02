"""
Together AI LLM Service for Mutual Fund Data Extraction
Extracts portfolio data from Excel sheets using Together AI models
"""

import pandas as pd
import json
import re
from typing import Optional, Dict, Any
from pathlib import Path
import sys

# Add parent directory to path to find external modules
sys.path.insert(0, str(Path(__file__).parent.parent))

try:
    from together import Together
except ImportError:
    Together = None
    print("⚠️  Together AI not installed. Run: pip install together")


class TogetherLLMService:
    """Service for extracting mutual fund data using Together AI LLM"""
    
    def __init__(self, api_key: str = None):
        """
        Initialize Together AI service
        
        Args:
            api_key: Together AI API key
        """
        if not Together:
            raise ImportError("Together AI package not installed. Run: pip install together")
            
        self.api_key = api_key or "bff39f38ee07df9a08ff8d2e7279b9d7223ab3f283a30bc39590d36f77dbd2fd"
        self.client = Together(api_key=self.api_key)
        
        # Available models to try
        self.models = [
            "openai/gpt-oss-20b",
            "meta-llama/Llama-3.3-70B-Instruct-Turbo-Free",
            "deepseek-ai/DeepSeek-V3.1",
            "deepseek-ai/DeepSeek-R1-0528-tput",
            "google/gemma-3n-E4B-it"
        ]
        self.current_model = self.models[0]  # Default model
    
    def read_sheet_as_text(self, file_path: str, sheet_name: str) -> Optional[str]:
        """
        Read Excel sheet and convert to clean text format
        
        Args:
            file_path: Path to Excel file
            sheet_name: Name of the sheet to read
            
        Returns:
            Clean text representation of the sheet or None if error
        """
        try:
            print(f"📖 Reading sheet '{sheet_name}' from {file_path}")
            
            # Read Excel with header detection
            df = pd.read_excel(file_path, sheet_name=sheet_name, engine='openpyxl')
            
            # Drop completely empty rows/columns
            df.dropna(how='all', axis=1, inplace=True)
            df.dropna(how='all', inplace=True)
            
            print(f"📊 Sheet dimensions: {df.shape[0]} rows x {df.shape[1]} columns")
            
            # Convert to clean markdown-style table if available, otherwise string
            if hasattr(df, 'to_markdown'):
                return df.to_markdown(index=False)
            else:
                return str(df)
                
        except Exception as e:
            print(f"❌ Error reading sheet '{sheet_name}': {e}")
            return None
    
    def extract_json_from_text(self, text: str) -> Optional[str]:
        """
        Extract JSON content from LLM response that might contain extra text
        
        Args:
            text: Raw LLM response text
            
        Returns:
            Clean JSON string or None if not found
        """
        # Try to find JSON object in the text
        patterns = [
            r'\{.*\}',  # Simple JSON object
            r'```json\s*(\{.*?\})\s*```',  # JSON in code blocks
            r'```\s*(\{.*?\})\s*```',  # JSON in generic code blocks
        ]
        
        for pattern in patterns:
            matches = re.findall(pattern, text, re.DOTALL)
            if matches:
                json_str = matches[0] if isinstance(matches[0], str) else matches[0]
                try:
                    # Test if it's valid JSON
                    json.loads(json_str)
                    return json_str
                except json.JSONDecodeError:
                    continue
        
        # If no pattern matches, try to find the largest JSON-like structure
        brace_count = 0
        start_idx = -1
        best_json = ""
        
        for i, char in enumerate(text):
            if char == '{':
                if brace_count == 0:
                    start_idx = i
                brace_count += 1
            elif char == '}':
                brace_count -= 1
                if brace_count == 0 and start_idx != -1:
                    candidate = text[start_idx:i+1]
                    try:
                        json.loads(candidate)
                        if len(candidate) > len(best_json):
                            best_json = candidate
                    except json.JSONDecodeError:
                        pass
        
        return best_json if best_json else None
    
    def extract_json_from_table(self, table_text: str, sheet_name: str = "unknown") -> Dict[str, Any]:
        """
        Extract portfolio JSON from table text using LLM
        
        Args:
            table_text: Text representation of the table
            sheet_name: Name of the source sheet
            
        Returns:
            Extracted portfolio data as dictionary
        """
        prompt = f"""
You are a financial data parser. Extract **ALL** stock holdings from the table below.

Rules:
- Return ONLY a JSON object with:
  - mutual_fund_name: string
  - portfolio_date: "March 2025"
  - total_holdings: number
  - portfolio_holdings: list of all stocks with:
      - name_of_instrument (string)
      - isin_code (string)
      - percentage_to_nav (string with % sign)
- DO NOT summarize, skip, or truncate.
- Extract **every single stock** in the table.
- If quantity or value has commas (e.g., 457,329), convert to number: 457329.
- Make sure you list all stocks in portfolio_holdings array
- total_holdings should match the count of items in portfolio_holdings array

Here is the equity portfolio from sheet {sheet_name}:
{table_text}

Return the JSON object only.
"""

        print(f"📝 Prompt length: {len(prompt)} characters")
        print(f"🤖 Using model: {self.current_model}")
        
        try:
            response = self.client.chat.completions.create(
                model=self.current_model,
                messages=[
                    {"role": "system", "content": "You are a precise financial data extractor. Return only JSON."},
                    {"role": "user", "content": prompt}
                ],
                max_tokens=50000
            )
            
            raw_output = response.choices[0].message.content.strip()
            print(f"📄 Response length: {len(raw_output)} characters")
            
            # Try to extract clean JSON from the response
            json_str = self.extract_json_from_text(raw_output)
            
            if json_str:
                try:
                    parsed_json = json.loads(json_str)
                    print("✅ Successfully extracted and parsed JSON")
                    return parsed_json
                except json.JSONDecodeError as e:
                    print(f"❌ JSON parsing failed: {e}")
                    print("📝 Saving raw output for debugging...")
                    self._save_debug_output(raw_output, sheet_name)
                    raise
            else:
                print("❌ No valid JSON found in LLM response")
                print("📝 Saving raw output for debugging...")
                self._save_debug_output(raw_output, sheet_name)
                raise ValueError("No valid JSON found in LLM response")
                
        except Exception as e:
            print(f"❌ API call failed: {str(e)}")
            raise
    
    def _save_debug_output(self, raw_output: str, sheet_name: str):
        """Save raw LLM output for debugging"""
        debug_file = f"debug_llm_output_{sheet_name}.txt"
        with open(debug_file, 'w', encoding='utf-8') as f:
            f.write(raw_output)
        print(f"🐛 Debug output saved to {debug_file}")
    
    def extract_portfolio_from_excel(self, excel_file: str, sheet_name: str, output_file: str = None) -> Dict[str, Any]:
        """
        Complete workflow: Read Excel sheet and extract portfolio JSON
        
        Args:
            excel_file: Path to Excel file
            sheet_name: Name of sheet to process
            output_file: Optional output file path
            
        Returns:
            Extracted portfolio data as dictionary
        """
        print(f"🚀 Starting extraction from {excel_file}, sheet '{sheet_name}'")
        
        # Step 1: Read Excel sheet
        table_text = self.read_sheet_as_text(excel_file, sheet_name)
        if not table_text:
            raise ValueError(f"Failed to read sheet '{sheet_name}' from {excel_file}")
        
        # Step 2: Extract JSON via LLM
        print("🧠 Sending to LLM for JSON extraction...")
        portfolio_data = self.extract_json_from_table(table_text, sheet_name)
        
        # Step 3: Validate and save
        if portfolio_data:
            print(f"✅ Successfully extracted portfolio: {portfolio_data.get('mutual_fund_name', 'Unknown')}")
            print(f"📊 Total holdings: {portfolio_data.get('total_holdings', 0)}")
            
            # Save to file if specified
            if output_file:
                output_path = Path(output_file)
                with open(output_path, 'w', encoding='utf-8') as f:
                    json.dump(portfolio_data, f, indent=2, ensure_ascii=False)
                print(f"💾 Saved to {output_path}")
            
            return portfolio_data
        else:
            raise ValueError("Failed to extract portfolio data")
    
    def change_model(self, model_name: str = None):
        """
        Change the LLM model being used
        
        Args:
            model_name: Name of the model to use, or None to cycle to next
        """
        if model_name and model_name in self.models:
            self.current_model = model_name
        else:
            # Cycle to next model
            current_idx = self.models.index(self.current_model)
            self.current_model = self.models[(current_idx + 1) % len(self.models)]
        
        print(f"🔄 Switched to model: {self.current_model}")


def main():
    """
    Example usage of the TogetherLLMService
    """
    # Configuration
    EXCEL_FILE = "c45b0-copy-of-motilal-hy-portfolio-march-2025.xlsx"
    SHEET_NAME = "YO17"  # Change this to any sheet: YO01, YO02, YO58, etc.
    
    try:
        # Initialize service
        service = TogetherLLMService()
        
        # Extract portfolio
        portfolio_data = service.extract_portfolio_from_excel(
            excel_file=EXCEL_FILE,
            sheet_name=SHEET_NAME,
            output_file=f"{SHEET_NAME}_extracted.json"
        )
        
        # Pretty print results
        print("\n🎉 Extraction complete!")
        print("=" * 50)
        print(json.dumps(portfolio_data, indent=2))
        
    except Exception as e:
        print(f"❌ Extraction failed: {str(e)}")


if __name__ == "__main__":
    main()