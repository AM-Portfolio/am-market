#!/bin/bash
# MongoDB initialization script
# Creates the mutual_funds database and collections with proper indexes

echo "🚀 Initializing MongoDB for AM Parser..."

# Switch to the mutual_funds database
mongosh <<EOF
use mutual_funds

// Create the portfolios collection with schema validation
db.createCollection("portfolios", {
  validator: {
    \$jsonSchema: {
      bsonType: "object",
      required: ["mutual_fund_name", "portfolio_date", "total_holdings", "portfolio_holdings"],
      properties: {
        mutual_fund_name: {
          bsonType: "string",
          description: "Name of the mutual fund - required"
        },
        portfolio_date: {
          bsonType: "string", 
          description: "Portfolio date - required"
        },
        total_holdings: {
          bsonType: "int",
          minimum: 0,
          description: "Total number of holdings - required"
        },
        portfolio_holdings: {
          bsonType: "array",
          description: "Array of holdings - required",
          items: {
            bsonType: "object",
            required: ["name_of_instrument", "isin_code", "percentage_to_nav"],
            properties: {
              name_of_instrument: {
                bsonType: "string",
                description: "Name of the security"
              },
              isin_code: {
                bsonType: "string",
                description: "ISIN code of the security"
              },
              percentage_to_nav: {
                bsonType: "string",
                description: "Percentage allocation to NAV"
              }
            }
          }
        },
        created_at: {
          bsonType: "string",
          description: "Creation timestamp"
        },
        updated_at: {
          bsonType: "string", 
          description: "Last update timestamp"
        }
      }
    }
  }
})

// Create indexes for better performance
db.portfolios.createIndex({ "mutual_fund_name": 1, "portfolio_date": 1 }, { unique: true })
db.portfolios.createIndex({ "mutual_fund_name": 1 })
db.portfolios.createIndex({ "portfolio_date": 1 })
db.portfolios.createIndex({ "portfolio_holdings.isin_code": 1 })
db.portfolios.createIndex({ "updated_at": -1 })

// Create a summary collection for aggregated data
db.createCollection("fund_summaries")
db.fund_summaries.createIndex({ "fund_name": 1 }, { unique: true })

print("✅ AM Parser MongoDB initialization complete!")
print("📋 Collections created:")
print("  - portfolios (with schema validation)")
print("  - fund_summaries")
print("🔍 Indexes created for optimal query performance")

EOF
