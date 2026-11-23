import time
from sqlalchemy import Column, String, Integer, BigInteger, Boolean, Text
from backend.database import Base

class TaskModel(Base):
    __tablename__ = "tasks"

    id = Column(String(64), primary_key=True, index=True)
    instruction = Column(Text, nullable=False)
    status = Column(String(32), default="QUEUED", index=True) # QUEUED, RUNNING, WAITING_APPROVAL, COMPLETED, FAILED, CANCELLED
    total_steps = Column(Integer, default=0)
    current_step = Column(Integer, default=0)
    retry_count = Column(Integer, default=0)
    created_at = Column(BigInteger, default=lambda: int(time.time() * 1000))
    completed_at = Column(BigInteger, nullable=True)
    final_result = Column(Text, nullable=True)
    error_message = Column(Text, nullable=True)
    pending_action = Column(Text, nullable=True)
    requires_approval = Column(Boolean, default=False)

class TaskStepModel(Base):
    __tablename__ = "task_steps"

    id = Column(Integer, primary_key=True, autoincrement=True)
    task_id = Column(String(64), index=True)
    step_number = Column(Integer)
    tool_name = Column(String(64))
    tool_input = Column(Text)
    tool_output = Column(Text, nullable=True)
    status = Column(String(32), default="RUNNING")
    timestamp = Column(BigInteger, default=lambda: int(time.time() * 1000))

class MemoryModel(Base):
    __tablename__ = "memories"

    id = Column(Integer, primary_key=True, autoincrement=True)
    key = Column(String(128), index=True)
    content = Column(Text, nullable=False)
    category = Column(String(64), default="general")
    created_at = Column(BigInteger, default=lambda: int(time.time() * 1000))
    updated_at = Column(BigInteger, default=lambda: int(time.time() * 1000))

class FileModel(Base):
    __tablename__ = "files"

    id = Column(String(64), primary_key=True)
    file_name = Column(String(256))
    file_type = Column(String(64))
    file_size = Column(BigInteger)
    drive_file_id = Column(String(128), nullable=True)
    task_id = Column(String(64), nullable=True)
    uploaded_at = Column(BigInteger, default=lambda: int(time.time() * 1000))
    download_url = Column(Text, nullable=True)
    is_synced = Column(Boolean, default=False)
