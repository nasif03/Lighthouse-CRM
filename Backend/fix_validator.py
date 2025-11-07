"""Quick fix script to update MongoDB validator - run this manually if needed"""
import sys
import os
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

from config.database import update_validators

if __name__ == "__main__":
    print("Updating MongoDB validator to support orgId as string or array...")
    update_validators()
    print("Done! You can now create organizations.")

