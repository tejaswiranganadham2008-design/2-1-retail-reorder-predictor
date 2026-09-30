#!/usr/bin/env python3
"""
=============================================================================
Retail Reorder Point Predictor - Machine Learning / Forecasting Module
Subject: AI & Python Programming (II B.Tech Mini Project)
Team: R. Tejaswi (Lead), E. Gayathri, M. Purna Satya Sri, N. Vasantha Lakshmi, E. Ganga
=============================================================================

Description:
This module reads historical daily sales data from `data/sales.csv`, computes
a 3-day Moving Average (MA-3) demand forecast for each SKU, and outputs the
results to `data/forecast.csv`.

Mathematical Formula:
    Moving Average (Window = 3):
    Forecast(t+1) = (Sales[t] + Sales[t-1] + Sales[t-2]) / 3.0

Time Complexity: O(N * M) where N = number of SKUs, M = sales days (O(1) window lookup)
Space Complexity: O(N) for storing forecast records
=============================================================================
"""

import os
import sys
import csv

def compute_forecast(sales_file_path="data/sales.csv", output_file_path="data/forecast.csv", window=3):
    """
    Reads daily sales data and computes Moving Average demand forecast.
    
    Parameters:
        sales_file_path (str): Path to input sales CSV
        output_file_path (str): Path to output forecast CSV
        window (int): Moving average window size (default: 3 days)
        
    Returns:
        int: Number of SKUs successfully forecasted, or -1 on error
    """
    print("=" * 60)
    print(" [AI/Python Forecast Engine] Starting Demand Prediction...")
    print(f" [INFO] Input file:  {sales_file_path}")
    print(f" [INFO] Output file: {output_file_path}")
    print(f" [INFO] Algorithm:   Moving Average (Window = {window})")
    print("=" * 60)

    # Step 1: Check if input sales file exists
    if not os.path.exists(sales_file_path):
        print(f" [ERROR] Input sales file not found at: '{sales_file_path}'", file=sys.stderr)
        return -1

    forecast_results = []

    # Step 2: Read historical sales data
    try:
        with open(sales_file_path, mode='r', newline='', encoding='utf-8') as f:
            reader = csv.reader(f)
            
            # Read header row
            try:
                header = next(reader)
            except StopIteration:
                print(" [ERROR] Sales file is completely empty.", file=sys.stderr)
                return -1

            row_count = 0
            for row_idx, row in enumerate(reader, start=2):
                # Skip empty or whitespace lines
                if not row or all(field.strip() == '' for field in row):
                    continue

                if len(row) < 2:
                    print(f" [WARN] Line {row_idx}: Skipped malformed row with fewer than 2 columns.", file=sys.stderr)
                    continue

                sku_id = row[0].strip()
                sku_name = row[1].strip()

                # Extract numerical sales history (from column index 2 onwards)
                raw_sales = row[2:]
                sales_values = []
                for val in raw_sales:
                    val = val.strip()
                    if val != '':
                        try:
                            sales_values.append(float(val))
                        except ValueError:
                            pass # Skip non-numeric values gracefully

                # Step 3: Compute moving average for available window
                total_days = len(sales_values)
                if total_days == 0:
                    # Edge Case: No sales data available
                    forecast = 0.0
                    last_days_str = "[]"
                    formula = "No sales data available = 0.00"
                elif total_days < window:
                    # Edge Case: Fewer than 'window' days available
                    recent_sales = sales_values
                    avg = sum(recent_sales) / len(recent_sales)
                    forecast = round(avg, 2)
                    recent_ints = [int(v) if v.is_integer() else v for v in recent_sales]
                    last_days_str = "[" + ", ".join(str(x) for x in recent_ints) + "]"
                    formula = f"({' + '.join(str(x) for x in recent_ints)}) / {len(recent_sales)} = {forecast:.2f}"
                else:
                    # Standard Case: Window = 3 days
                    recent_sales = sales_values[-window:]
                    avg = sum(recent_sales) / float(window)
                    forecast = round(avg, 2)
                    recent_ints = [int(v) if v.is_integer() else v for v in recent_sales]
                    last_days_str = "[" + ", ".join(str(x) for x in recent_ints) + "]"
                    formula = f"({' + '.join(str(x) for x in recent_ints)}) / {window} = {forecast:.2f}"

                forecast_results.append({
                    "sku_id": sku_id,
                    "sku_name": sku_name,
                    "last_3_days": last_days_str,
                    "forecast_demand": f"{forecast:.2f}",
                    "formula": formula
                })
                row_count += 1

        print(f" [INFO] Processed {row_count} SKU sales histories successfully.")

    except Exception as e:
        print(f" [ERROR] Exception while reading sales file: {e}", file=sys.stderr)
        return -1

    # Step 4: Ensure output directory exists
    output_dir = os.path.dirname(output_file_path)
    if output_dir and not os.path.exists(output_dir):
        try:
            os.makedirs(output_dir, exist_ok=True)
        except Exception as e:
            print(f" [ERROR] Could not create directory '{output_dir}': {e}", file=sys.stderr)
            return -1

    # Step 5: Write results to forecast.csv
    try:
        with open(output_file_path, mode='w', newline='', encoding='utf-8') as f:
            fieldnames = ["sku_id", "sku_name", "last_3_days", "forecast_demand", "formula"]
            writer = csv.DictWriter(f, fieldnames=fieldnames)
            writer.writeheader()
            for record in forecast_results:
                writer.writerow(record)

        print(f" [SUCCESS] Successfully generated forecast for {len(forecast_results)} SKUs in: '{output_file_path}'")
        print("=" * 60)
        return len(forecast_results)

    except Exception as e:
        print(f" [ERROR] Exception while writing forecast file: {e}", file=sys.stderr)
        return -1


if __name__ == "__main__":
    # Allow custom paths from CLI arguments if provided by Java ProcessBuilder
    sales_path = sys.argv[1] if len(sys.argv) > 1 else "data/sales.csv"
    forecast_path = sys.argv[2] if len(sys.argv) > 2 else "data/forecast.csv"
    
    status = compute_forecast(sales_path, forecast_path)
    if status < 0:
        sys.exit(1)
    else:
        sys.exit(0)
