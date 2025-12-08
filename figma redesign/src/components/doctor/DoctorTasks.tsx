import React, { useState, useEffect } from 'react';
import { Plus, CheckSquare, Calendar, AlertCircle, User } from 'lucide-react';
import { Card, CardContent } from '../ui/Card';
import { Badge } from '../ui/Badge';
import { Button } from '../ui/Button';
import { Input } from '../ui/Input';
import { Select } from '../ui/Select';
import { Modal } from '../ui/Modal';
import { EmptyState } from '../ui/EmptyState';
import { CardSkeleton } from '../ui/Skeleton';
import { tasksApi } from '../../utils/mockApi';
import type { Task, TaskStatus, TaskPriority } from '../../utils/mockData';

const priorityColors = {
  LOW: 'bg-neutral-100 text-neutral-700',
  MEDIUM: 'bg-warning-50 text-warning-700',
  HIGH: 'bg-danger-50 text-danger-700'
};

const statusOptions = [
  { value: 'ALL', label: 'All Statuses' },
  { value: 'TODO', label: 'To Do' },
  { value: 'IN_PROGRESS', label: 'In Progress' },
  { value: 'DONE', label: 'Done' }
];

export function DoctorTasks() {
  const [tasks, setTasks] = useState<Task[]>([]);
  const [loading, setLoading] = useState(true);
  const [statusFilter, setStatusFilter] = useState<TaskStatus | 'ALL'>('ALL');
  const [showAddModal, setShowAddModal] = useState(false);
  const [formData, setFormData] = useState({
    title: '',
    description: '',
    dueDate: '',
    priority: 'MEDIUM' as TaskPriority
  });

  useEffect(() => {
    loadTasks();
  }, [statusFilter]);

  const loadTasks = async () => {
    setLoading(true);
    try {
      const data = await tasksApi.listForDoctor('doc-1', {
        status: statusFilter === 'ALL' ? undefined : statusFilter
      });
      setTasks(data);
    } catch (error) {
      console.error('Failed to load tasks:', error);
    } finally {
      setLoading(false);
    }
  };

  const handleAddTask = async () => {
    if (!formData.title || !formData.dueDate) {
      alert('Please fill in all required fields');
      return;
    }

    await tasksApi.create({
      ...formData,
      status: 'TODO'
    });

    setFormData({
      title: '',
      description: '',
      dueDate: '',
      priority: 'MEDIUM'
    });
    setShowAddModal(false);
    loadTasks();
  };

  const handleMarkComplete = async (task: Task) => {
    await tasksApi.update(task.id, { status: 'DONE' });
    loadTasks();
  };

  const groupedTasks = {
    TODO: tasks.filter(t => t.status === 'TODO'),
    IN_PROGRESS: tasks.filter(t => t.status === 'IN_PROGRESS'),
    DONE: tasks.filter(t => t.status === 'DONE')
  };

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div>
          <h2 className="text-neutral-900">Tasks</h2>
          <p className="text-neutral-600 mt-1">Manage your daily tasks and follow-ups</p>
        </div>
        <Button onClick={() => setShowAddModal(true)}>
          <Plus className="w-5 h-5" />
          Add Task
        </Button>
      </div>

      {/* Filters */}
      <Card>
        <CardContent className="!py-4">
          <div className="max-w-xs">
            <Select
              options={statusOptions}
              value={statusFilter}
              onChange={(value) => setStatusFilter(value as TaskStatus | 'ALL')}
            />
          </div>
        </CardContent>
      </Card>

      {/* Tasks List */}
      {loading ? (
        <div className="space-y-4">
          {[1, 2, 3].map(i => <CardSkeleton key={i} />)}
        </div>
      ) : tasks.length === 0 ? (
        <Card>
          <EmptyState
            icon={<CheckSquare className="w-8 h-8" />}
            title="No tasks found"
            description="Create your first task to stay organized."
            action={
              <Button onClick={() => setShowAddModal(true)}>
                <Plus className="w-5 h-5" />
                Add Task
              </Button>
            }
          />
        </Card>
      ) : (
        <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
          {/* To Do Column */}
          <div>
            <div className="flex items-center gap-2 mb-4">
              <div className="w-2 h-2 rounded-full bg-neutral-400" />
              <h3 className="text-neutral-900">To Do</h3>
              <span className="text-neutral-500">({groupedTasks.TODO.length})</span>
            </div>
            <div className="space-y-3">
              {groupedTasks.TODO.map(task => (
                <Card key={task.id}>
                  <CardContent className="!p-4">
                    <div className="flex items-start justify-between mb-2">
                      <h4 className="text-neutral-900 flex-1">{task.title}</h4>
                      <Badge className={`${priorityColors[task.priority]} text-xs ml-2`}>
                        {task.priority}
                      </Badge>
                    </div>
                    
                    {task.description && (
                      <p className="text-sm text-neutral-600 mb-3">{task.description}</p>
                    )}

                    <div className="space-y-2 text-sm mb-3">
                      <div className="flex items-center gap-2 text-neutral-600">
                        <Calendar className="w-4 h-4" />
                        <span>{new Date(task.dueDate).toLocaleDateString()}</span>
                      </div>
                      
                      {task.relatedPatientName && (
                        <div className="flex items-center gap-2 text-neutral-600">
                          <User className="w-4 h-4" />
                          <span>{task.relatedPatientName}</span>
                        </div>
                      )}
                    </div>

                    <Button
                      size="sm"
                      variant="secondary"
                      onClick={() => handleMarkComplete(task)}
                      className="w-full"
                    >
                      <CheckSquare className="w-4 h-4" />
                      Mark Complete
                    </Button>
                  </CardContent>
                </Card>
              ))}
            </div>
          </div>

          {/* In Progress Column */}
          <div>
            <div className="flex items-center gap-2 mb-4">
              <div className="w-2 h-2 rounded-full bg-secondary-500" />
              <h3 className="text-neutral-900">In Progress</h3>
              <span className="text-neutral-500">({groupedTasks.IN_PROGRESS.length})</span>
            </div>
            <div className="space-y-3">
              {groupedTasks.IN_PROGRESS.map(task => (
                <Card key={task.id}>
                  <CardContent className="!p-4">
                    <div className="flex items-start justify-between mb-2">
                      <h4 className="text-neutral-900 flex-1">{task.title}</h4>
                      <Badge className={`${priorityColors[task.priority]} text-xs ml-2`}>
                        {task.priority}
                      </Badge>
                    </div>
                    
                    {task.description && (
                      <p className="text-sm text-neutral-600 mb-3">{task.description}</p>
                    )}

                    <div className="space-y-2 text-sm mb-3">
                      <div className="flex items-center gap-2 text-neutral-600">
                        <Calendar className="w-4 h-4" />
                        <span>{new Date(task.dueDate).toLocaleDateString()}</span>
                      </div>
                      
                      {task.relatedPatientName && (
                        <div className="flex items-center gap-2 text-neutral-600">
                          <User className="w-4 h-4" />
                          <span>{task.relatedPatientName}</span>
                        </div>
                      )}
                    </div>

                    <Button
                      size="sm"
                      variant="secondary"
                      onClick={() => handleMarkComplete(task)}
                      className="w-full"
                    >
                      <CheckSquare className="w-4 h-4" />
                      Mark Complete
                    </Button>
                  </CardContent>
                </Card>
              ))}
            </div>
          </div>

          {/* Done Column */}
          <div>
            <div className="flex items-center gap-2 mb-4">
              <div className="w-2 h-2 rounded-full bg-success-500" />
              <h3 className="text-neutral-900">Done</h3>
              <span className="text-neutral-500">({groupedTasks.DONE.length})</span>
            </div>
            <div className="space-y-3">
              {groupedTasks.DONE.map(task => (
                <Card key={task.id} className="opacity-75">
                  <CardContent className="!p-4">
                    <div className="flex items-start justify-between mb-2">
                      <h4 className="text-neutral-900 flex-1 line-through">{task.title}</h4>
                      <Badge className={`${priorityColors[task.priority]} text-xs ml-2`}>
                        {task.priority}
                      </Badge>
                    </div>
                    
                    {task.description && (
                      <p className="text-sm text-neutral-600 mb-2">{task.description}</p>
                    )}

                    <div className="flex items-center gap-2 text-sm text-neutral-600">
                      <Calendar className="w-4 h-4" />
                      <span>{new Date(task.dueDate).toLocaleDateString()}</span>
                    </div>
                  </CardContent>
                </Card>
              ))}
            </div>
          </div>
        </div>
      )}

      {/* Add Task Modal */}
      <Modal
        isOpen={showAddModal}
        onClose={() => setShowAddModal(false)}
        title="Add New Task"
        footer={
          <>
            <Button variant="secondary" onClick={() => setShowAddModal(false)}>
              Cancel
            </Button>
            <Button onClick={handleAddTask}>
              Add Task
            </Button>
          </>
        }
      >
        <div className="space-y-4">
          <Input
            label="Task Title"
            value={formData.title}
            onChange={(e) => setFormData({ ...formData, title: e.target.value })}
            placeholder="e.g., Review lab results"
          />
          
          <div>
            <label className="block text-neutral-700 mb-2">Description</label>
            <textarea
              value={formData.description}
              onChange={(e) => setFormData({ ...formData, description: e.target.value })}
              placeholder="Add task details..."
              rows={3}
              className="w-full px-4 py-2.5 bg-white border border-neutral-300 rounded-lg focus:border-primary-600 focus:ring-2 focus:ring-primary-100 outline-none"
            />
          </div>

          <Input
            label="Due Date"
            type="date"
            value={formData.dueDate}
            onChange={(e) => setFormData({ ...formData, dueDate: e.target.value })}
          />

          <Select
            label="Priority"
            options={[
              { value: 'LOW', label: 'Low' },
              { value: 'MEDIUM', label: 'Medium' },
              { value: 'HIGH', label: 'High' }
            ]}
            value={formData.priority}
            onChange={(value) => setFormData({ ...formData, priority: value as TaskPriority })}
          />
        </div>
      </Modal>
    </div>
  );
}
