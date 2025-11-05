import Input from '../components/ui/Input';
import Button from '../components/ui/Button';
import { useAuthStore } from '../store/authStore';
import { useNavigate } from 'react-router-dom';
import { FormEvent, useState } from 'react';

export default function Login() {
  const navigate = useNavigate();
  const { login } = useAuthStore();
  const [email, setEmail] = useState('alex@example.com');
  const [name, setName] = useState('Alex Admin');

  function onSubmit(e: FormEvent) {
    e.preventDefault();
    login({ id: '1', name, email });
    navigate('/');
  }

  return (
    <div className="min-h-full grid place-items-center">
      <form onSubmit={onSubmit} className="w-full max-w-sm rounded border border-gray-200 p-6">
        <div className="text-xl font-semibold mb-4">Login</div>
        <div className="flex flex-col gap-3">
          <label className="text-sm">
            <div className="mb-1 text-gray-600">Name</div>
            <Input value={name} onChange={(e) => setName(e.target.value)} />
          </label>
          <label className="text-sm">
            <div className="mb-1 text-gray-600">Email</div>
            <Input type="email" value={email} onChange={(e) => setEmail(e.target.value)} />
          </label>
          <Button type="submit">Continue</Button>
        </div>
      </form>
    </div>
  );
}


