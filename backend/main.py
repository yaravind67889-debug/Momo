import os
import uuid
import time
from typing import List, Optional
from fastapi import FastAPI, Depends, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel
from sqlalchemy.orm import Session

from backend.database import engine, Base, get_db
from backend.models import TaskModel, TaskStepModel, MemoryModel, FileModel

# Create database tables
Base.metadata.create_all(bind=engine)

app = FastAPI(title="Momo AI — Dedicated Agent Backend")

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

class TaskCreateRequest(BaseModel):
    instruction: str

class MemoryCreateRequest(BaseModel):
    key: str
    content: str
    category: str = "general"

class ApprovalDecisionRequest(BaseModel):
    approved: bool
    reason: Optional[str] = None

@app.get("/api/health")
def health():
    return {
        "status": "online",
        "service": "momo-agent-backend",
        "timestamp": int(time.time() * 1000)
    }

@app.post("/api/tasks")
def create_task(req: TaskCreateRequest, db: Session = Depends(get_db)):
    task_id = str(uuid.uuid4())
    task = TaskModel(
        id=task_id,
        instruction=req.instruction,
        status="QUEUED",
        created_at=int(time.time() * 1000)
    )
    db.add(task)
    db.commit()
    db.refresh(task)
    return task

@app.get("/api/tasks")
def list_tasks(db: Session = Depends(get_db)):
    return db.query(TaskModel).order_by(TaskModel.created_at.desc()).all()

@app.get("/api/tasks/{task_id}")
def get_task(task_id: str, db: Session = Depends(get_db)):
    task = db.query(TaskModel).filter(TaskModel.id == task_id).first()
    if not task:
        raise HTTPException(status_code=404, detail="Task not found")
    steps = db.query(TaskStepModel).filter(TaskStepModel.task_id == task_id).all()
    return {"task": task, "steps": steps}

@app.post("/api/tasks/{task_id}/cancel")
def cancel_task(task_id: str, db: Session = Depends(get_db)):
    task = db.query(TaskModel).filter(TaskModel.id == task_id).first()
    if not task:
        raise HTTPException(status_code=404, detail="Task not found")
    task.status = "CANCELLED"
    task.completed_at = int(time.time() * 1000)
    task.error_message = "Cancelled by owner"
    db.commit()
    return task

@app.post("/api/tasks/{task_id}/approval")
def decide_approval(task_id: str, req: ApprovalDecisionRequest, db: Session = Depends(get_db)):
    task = db.query(TaskModel).filter(TaskModel.id == task_id).first()
    if not task:
        raise HTTPException(status_code=404, detail="Task not found")
    if req.approved:
        task.status = "RUNNING"
        task.requires_approval = False
        task.pending_action = None
    else:
        task.status = "FAILED"
        task.requires_approval = False
        task.error_message = req.reason or "Action rejected by owner"
        task.completed_at = int(time.time() * 1000)
    db.commit()
    return task

@app.get("/api/memories")
def list_memories(query: Optional[str] = None, db: Session = Depends(get_db)):
    q = db.query(MemoryModel)
    if query:
        q = q.filter(MemoryModel.content.contains(query) | MemoryModel.key.contains(query))
    return q.order_by(MemoryModel.updated_at.desc()).all()

@app.post("/api/memories")
def add_memory(req: MemoryCreateRequest, db: Session = Depends(get_db)):
    mem = MemoryModel(
        key=req.key,
        content=req.content,
        category=req.category,
        created_at=int(time.time() * 1000),
        updated_at=int(time.time() * 1000)
    )
    db.add(mem)
    db.commit()
    db.refresh(mem)
    return mem

@app.delete("/api/memories/{memory_id}")
def delete_memory(memory_id: int, db: Session = Depends(get_db)):
    mem = db.query(MemoryModel).filter(MemoryModel.id == memory_id).first()
    if mem:
        db.delete(mem)
        db.commit()
    return {"deleted": True}

@app.get("/api/files")
def list_files(db: Session = Depends(get_db)):
    return db.query(FileModel).order_by(FileModel.uploaded_at.desc()).all()
