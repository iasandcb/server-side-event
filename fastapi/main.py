import asyncio
from fastapi import FastAPI
from fastapi.responses import StreamingResponse
import time

app = FastAPI()

async def event_stream():
    # Simulate AI processing steps
    steps = [
        "Step 1: Data loading complete...",
        "Step 2: Preprocessing data...",
        "Step 3: Model inference started...",
        "Step 4: Calculating results...",
        "Step 5: Postprocessing results...",
        "Step 6: Analysis complete."
    ]
    for step in steps:
        # Simulate work being done
        await asyncio.sleep(1)
        # Send data in SSE format: data: [message]\n\n
        yield f"data: {step}\n\n"
        print(f"FastAPI sent: {step}") # Log on server side

    # Optional: Send a final message or signal completion
    await asyncio.sleep(1)
    yield "data: FINISHED\n\n"
    print("FastAPI stream finished.")

@app.get("/stream")
async def stream_events():
    print("Client connected to stream.")
    # Return StreamingResponse with text/event-stream media type
    return StreamingResponse(event_stream(), media_type="text/event-stream")

if __name__ == "__main__":
    import uvicorn
    # Run on port 8000
    uvicorn.run(app, host="0.0.0.0", port=8000)