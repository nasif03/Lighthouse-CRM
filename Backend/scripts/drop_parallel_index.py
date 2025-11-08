"""Drop the problematic parallel array index"""
from config.database import db

try:
    indexes = db.users.index_information()
    if 'orgId_roleIds_idx' in indexes:
        db.users.drop_index('orgId_roleIds_idx')
        print("Dropped orgId_roleIds_idx index")
    else:
        print("Index does not exist")
except Exception as e:
    print(f"Error: {e}")

