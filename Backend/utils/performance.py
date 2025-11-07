"""Performance monitoring utilities"""
import time
import functools
from contextlib import contextmanager
from typing import Callable, Any

def log_timing(operation_name: str, threshold_ms: float = 100.0):
    """Decorator to log execution time of async functions"""
    def decorator(func: Callable) -> Callable:
        @functools.wraps(func)
        async def async_wrapper(*args, **kwargs):
            start_time = time.perf_counter()
            try:
                result = await func(*args, **kwargs)
                elapsed_ms = (time.perf_counter() - start_time) * 1000
                if elapsed_ms > threshold_ms:
                    print(f"⏱️  [{operation_name}] took {elapsed_ms:.2f}ms (SLOW - threshold: {threshold_ms}ms)")
                else:
                    print(f"⏱️  [{operation_name}] took {elapsed_ms:.2f}ms")
                return result
            except Exception as e:
                elapsed_ms = (time.perf_counter() - start_time) * 1000
                print(f"⏱️  [{operation_name}] failed after {elapsed_ms:.2f}ms: {str(e)}")
                raise
        
        @functools.wraps(func)
        def sync_wrapper(*args, **kwargs):
            start_time = time.perf_counter()
            try:
                result = func(*args, **kwargs)
                elapsed_ms = (time.perf_counter() - start_time) * 1000
                if elapsed_ms > threshold_ms:
                    print(f"⏱️  [{operation_name}] took {elapsed_ms:.2f}ms (SLOW - threshold: {threshold_ms}ms)")
                else:
                    print(f"⏱️  [{operation_name}] took {elapsed_ms:.2f}ms")
                return result
            except Exception as e:
                elapsed_ms = (time.perf_counter() - start_time) * 1000
                print(f"⏱️  [{operation_name}] failed after {elapsed_ms:.2f}ms: {str(e)}")
                raise
        
        # Return appropriate wrapper based on whether function is async
        if functools.iscoroutinefunction(func):
            return async_wrapper
        return sync_wrapper
    return decorator

@contextmanager
def time_operation(operation_name: str, threshold_ms: float = 100.0):
    """Context manager to time a block of code"""
    start_time = time.perf_counter()
    try:
        yield
    finally:
        elapsed_ms = (time.perf_counter() - start_time) * 1000
        if elapsed_ms > threshold_ms:
            print(f"⏱️  [{operation_name}] took {elapsed_ms:.2f}ms (SLOW - threshold: {threshold_ms}ms)")
        else:
            print(f"⏱️  [{operation_name}] took {elapsed_ms:.2f}ms")

def time_database_query(collection_name: str, operation: str = "find"):
    """Context manager specifically for database queries"""
    return time_operation(f"DB[{collection_name}].{operation}", threshold_ms=50.0)

