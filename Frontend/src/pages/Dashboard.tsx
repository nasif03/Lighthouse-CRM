import Card, { CardContent, CardHeader } from '../components/ui/Card';
import { useState, useEffect, useCallback } from 'react';
import { useAuthStore } from '../store/authStore';
import { apiGet } from '../utils/api';
import { Link } from 'react-router-dom';

type DashboardStats = {
  summary: {
    totalLeads: number;
    totalContacts: number;
    totalDeals: number;
    totalAccounts: number;
    recentLeads: number;
    recentContacts: number;
    recentDeals: number;
    recentActivities: number;
    totalDealValue: number;
    wonDealValue: number;
    conversionRate: number;
  };
  leadsByStatus: Record<string, number>;
  dealsByStage: Record<string, number>;
};

type RecentItem = {
  id: string;
  name: string;
  email?: string;
  status?: string;
  source?: string;
  amount?: number;
  currency?: string;
  stageId?: string;
  stageName?: string;
  title?: string;
  createdAt: string;
};

type RecentData = {
  recentLeads: RecentItem[];
  recentDeals: RecentItem[];
  recentContacts: RecentItem[];
};

// Helper function to format currency
const formatCurrency = (amount: number, currency: string = 'USD'): string => {
  return new Intl.NumberFormat('en-US', {
    style: 'currency',
    currency: currency,
    minimumFractionDigits: 0,
    maximumFractionDigits: 0,
  }).format(amount);
};

// Helper function to format relative time
const formatRelativeTime = (dateString: string): string => {
  const date = new Date(dateString);
  const now = new Date();
  const diffInSeconds = Math.floor((now.getTime() - date.getTime()) / 1000);
  
  if (diffInSeconds < 60) return 'Just now';
  if (diffInSeconds < 3600) return `${Math.floor(diffInSeconds / 60)}m ago`;
  if (diffInSeconds < 86400) return `${Math.floor(diffInSeconds / 3600)}h ago`;
  if (diffInSeconds < 604800) return `${Math.floor(diffInSeconds / 86400)}d ago`;
  
  return date.toLocaleDateString();
};

// Helper function to get status color
const getStatusColor = (status: string): string => {
  const colors: Record<string, string> = {
    new: 'bg-blue-100 text-blue-700',
    contacted: 'bg-yellow-100 text-yellow-700',
    qualified: 'bg-green-100 text-green-700',
    converted: 'bg-purple-100 text-purple-700',
    lost: 'bg-red-100 text-red-700',
  };
  return colors[status] || 'bg-gray-100 text-gray-700';
};

// Helper function to get stage color
const getStageColor = (stage: string): string => {
  const colors: Record<string, string> = {
    prospecting: 'bg-blue-100 text-blue-700',
    qualification: 'bg-yellow-100 text-yellow-700',
    proposal: 'bg-green-100 text-green-700',
    negotiation: 'bg-orange-100 text-orange-700',
    'closed-won': 'bg-purple-100 text-purple-700',
    'closed-lost': 'bg-red-100 text-red-700',
  };
  return colors[stage] || 'bg-gray-100 text-gray-700';
};

export default function Dashboard() {
  const { token } = useAuthStore();
  const [stats, setStats] = useState<DashboardStats | null>(null);
  const [recent, setRecent] = useState<RecentData | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const fetchDashboardData = useCallback(async () => {
    if (!token) {
      setIsLoading(false);
      return;
    }

    try {
      setIsLoading(true);
      setError(null);
      
      const [statsData, recentData] = await Promise.all([
        apiGet<DashboardStats>('/api/dashboard/stats', token),
        apiGet<RecentData>('/api/dashboard/recent', token),
      ]);
      
      setStats(statsData);
      setRecent(recentData);
    } catch (err: any) {
      if (err.message === 'Request cancelled') {
        return;
      }
      console.error('Error fetching dashboard data:', err);
      setError(err.message || 'Failed to load dashboard data');
    } finally {
      setIsLoading(false);
    }
  }, [token]);

  useEffect(() => {
    fetchDashboardData();
  }, [fetchDashboardData]);

  if (isLoading) {
    return (
      <div className="flex items-center justify-center h-64">
        <div className="text-gray-600">Loading dashboard...</div>
      </div>
    );
  }

  if (error && !stats) {
    return (
      <div className="flex items-center justify-center h-64">
        <div className="text-red-600">{error}</div>
      </div>
    );
  }

  return (
    <div className="flex flex-col gap-6">
      {/* Summary Cards */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
        <Card className="hover:shadow-md transition-shadow">
          <CardHeader className="flex items-center justify-between">
            <span>Total Leads</span>
            <span className="text-2xl">📋</span>
          </CardHeader>
          <CardContent>
            <div className="text-3xl font-bold text-gray-900">{stats?.summary.totalLeads || 0}</div>
            <div className="text-sm text-gray-500 mt-1">
              {stats?.summary.recentLeads || 0} new this week
            </div>
            <Link to="/leads" className="text-sm text-brand-600 hover:underline mt-2 inline-block">
              View all →
            </Link>
          </CardContent>
        </Card>

        <Card className="hover:shadow-md transition-shadow">
          <CardHeader className="flex items-center justify-between">
            <span>Total Contacts</span>
            <span className="text-2xl">👥</span>
          </CardHeader>
          <CardContent>
            <div className="text-3xl font-bold text-gray-900">{stats?.summary.totalContacts || 0}</div>
            <div className="text-sm text-gray-500 mt-1">
              {stats?.summary.recentContacts || 0} new this week
            </div>
            <Link to="/contacts" className="text-sm text-brand-600 hover:underline mt-2 inline-block">
              View all →
            </Link>
          </CardContent>
        </Card>

        <Card className="hover:shadow-md transition-shadow">
          <CardHeader className="flex items-center justify-between">
            <span>Total Deals</span>
            <span className="text-2xl">💰</span>
          </CardHeader>
          <CardContent>
            <div className="text-3xl font-bold text-gray-900">{stats?.summary.totalDeals || 0}</div>
            <div className="text-sm text-gray-500 mt-1">
              {stats?.summary.recentDeals || 0} new this week
            </div>
            <Link to="/deals" className="text-sm text-brand-600 hover:underline mt-2 inline-block">
              View all →
            </Link>
          </CardContent>
        </Card>

        <Card className="hover:shadow-md transition-shadow">
          <CardHeader className="flex items-center justify-between">
            <span>Total Accounts</span>
            <span className="text-2xl">🏢</span>
          </CardHeader>
          <CardContent>
            <div className="text-3xl font-bold text-gray-900">{stats?.summary.totalAccounts || 0}</div>
            <div className="text-sm text-gray-500 mt-1">Active accounts</div>
          </CardContent>
        </Card>
      </div>

      {/* Revenue & Conversion Cards */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
        <Card className="hover:shadow-md transition-shadow">
          <CardHeader>Total Deal Value</CardHeader>
          <CardContent>
            <div className="text-3xl font-bold text-gray-900">
              {formatCurrency(stats?.summary.totalDealValue || 0)}
            </div>
            <div className="text-sm text-gray-500 mt-1">All active deals</div>
          </CardContent>
        </Card>

        <Card className="hover:shadow-md transition-shadow">
          <CardHeader>Won Deal Value</CardHeader>
          <CardContent>
            <div className="text-3xl font-bold text-green-600">
              {formatCurrency(stats?.summary.wonDealValue || 0)}
            </div>
            <div className="text-sm text-gray-500 mt-1">Closed won deals</div>
          </CardContent>
        </Card>

        <Card className="hover:shadow-md transition-shadow">
          <CardHeader>Conversion Rate</CardHeader>
          <CardContent>
            <div className="text-3xl font-bold text-brand-600">
              {stats?.summary.conversionRate || 0}%
            </div>
            <div className="text-sm text-gray-500 mt-1">Leads converted</div>
          </CardContent>
        </Card>
      </div>

      {/* Leads & Deals Pipeline */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-4">
        <Card>
          <CardHeader>Leads by Status</CardHeader>
          <CardContent>
            <div className="space-y-3">
              {stats?.leadsByStatus && Object.entries(stats.leadsByStatus).map(([status, count]) => (
                <div key={status} className="flex items-center justify-between">
                  <div className="flex items-center gap-2">
                    <span className={`px-2 py-1 rounded text-xs font-medium ${getStatusColor(status)}`}>
                      {status.charAt(0).toUpperCase() + status.slice(1)}
                    </span>
                  </div>
                  <div className="flex items-center gap-3 flex-1 ml-4">
                    <div className="flex-1 bg-gray-200 rounded-full h-2">
                      <div
                        className={`h-2 rounded-full ${
                          status === 'new' ? 'bg-blue-500' :
                          status === 'contacted' ? 'bg-yellow-500' :
                          status === 'qualified' ? 'bg-green-500' :
                          status === 'converted' ? 'bg-purple-500' : 'bg-red-500'
                        }`}
                        style={{
                          width: `${stats.summary.totalLeads > 0 ? (count / stats.summary.totalLeads * 100) : 0}%`
                        }}
                      />
                    </div>
                    <span className="text-sm font-semibold text-gray-700 w-8 text-right">{count}</span>
                  </div>
                </div>
              ))}
            </div>
          </CardContent>
        </Card>

        <Card>
          <CardHeader>Deals by Stage</CardHeader>
          <CardContent>
            <div className="space-y-3">
              {stats?.dealsByStage && Object.entries(stats.dealsByStage).map(([stage, count]) => (
                <div key={stage} className="flex items-center justify-between">
                  <div className="flex items-center gap-2">
                    <span className={`px-2 py-1 rounded text-xs font-medium ${getStageColor(stage)}`}>
                      {stage.replace('-', ' ').replace(/\b\w/g, l => l.toUpperCase())}
                    </span>
                  </div>
                  <div className="flex items-center gap-3 flex-1 ml-4">
                    <div className="flex-1 bg-gray-200 rounded-full h-2">
                      <div
                        className={`h-2 rounded-full ${
                          stage === 'prospecting' ? 'bg-blue-500' :
                          stage === 'qualification' ? 'bg-yellow-500' :
                          stage === 'proposal' ? 'bg-green-500' :
                          stage === 'negotiation' ? 'bg-orange-500' :
                          stage === 'closed-won' ? 'bg-purple-500' : 'bg-red-500'
                        }`}
                        style={{
                          width: `${stats.summary.totalDeals > 0 ? (count / stats.summary.totalDeals * 100) : 0}%`
                        }}
                      />
                    </div>
                    <span className="text-sm font-semibold text-gray-700 w-8 text-right">{count}</span>
                  </div>
                </div>
              ))}
            </div>
          </CardContent>
        </Card>
      </div>

      {/* Recent Activity */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-4">
        <Card>
          <CardHeader className="flex items-center justify-between">
            <span>Recent Leads</span>
            <Link to="/leads" className="text-sm text-brand-600 hover:underline">
              View all
            </Link>
          </CardHeader>
          <CardContent>
            {recent?.recentLeads && recent.recentLeads.length > 0 ? (
              <div className="space-y-3">
                {recent.recentLeads.map((lead) => (
                  <div key={lead.id} className="flex items-start justify-between gap-2 pb-3 border-b border-gray-100 last:border-0">
                    <div className="flex-1 min-w-0">
                      <div className="font-medium text-sm text-gray-900 truncate">{lead.name}</div>
                      <div className="text-xs text-gray-500 truncate">{lead.email}</div>
                      <div className="flex items-center gap-2 mt-1">
                        <span className={`px-1.5 py-0.5 rounded text-xs ${getStatusColor(lead.status || '')}`}>
                          {lead.status}
                        </span>
                        {lead.source && (
                          <span className="text-xs text-gray-500">{lead.source}</span>
                        )}
                      </div>
                    </div>
                    <div className="text-xs text-gray-400 flex-shrink-0">
                      {formatRelativeTime(lead.createdAt)}
                    </div>
                  </div>
                ))}
              </div>
            ) : (
              <div className="text-center text-gray-500 text-sm py-4">No recent leads</div>
            )}
          </CardContent>
        </Card>

        <Card>
          <CardHeader className="flex items-center justify-between">
            <span>Recent Deals</span>
            <Link to="/deals" className="text-sm text-brand-600 hover:underline">
              View all
            </Link>
          </CardHeader>
          <CardContent>
            {recent?.recentDeals && recent.recentDeals.length > 0 ? (
              <div className="space-y-3">
                {recent.recentDeals.map((deal) => (
                  <div key={deal.id} className="flex items-start justify-between gap-2 pb-3 border-b border-gray-100 last:border-0">
                    <div className="flex-1 min-w-0">
                      <div className="font-medium text-sm text-gray-900 truncate">{deal.name}</div>
                      {deal.amount && (
                        <div className="text-sm font-semibold text-gray-700 mt-1">
                          {formatCurrency(deal.amount, deal.currency || 'USD')}
                        </div>
                      )}
                      {deal.stageName && (
                        <span className={`inline-block mt-1 px-1.5 py-0.5 rounded text-xs ${getStageColor(deal.stageId || '')}`}>
                          {deal.stageName}
                        </span>
                      )}
                    </div>
                    <div className="text-xs text-gray-400 flex-shrink-0">
                      {formatRelativeTime(deal.createdAt)}
                    </div>
                  </div>
                ))}
              </div>
            ) : (
              <div className="text-center text-gray-500 text-sm py-4">No recent deals</div>
            )}
          </CardContent>
        </Card>

        <Card>
          <CardHeader className="flex items-center justify-between">
            <span>Recent Contacts</span>
            <Link to="/contacts" className="text-sm text-brand-600 hover:underline">
              View all
            </Link>
          </CardHeader>
          <CardContent>
            {recent?.recentContacts && recent.recentContacts.length > 0 ? (
              <div className="space-y-3">
                {recent.recentContacts.map((contact) => (
                  <div key={contact.id} className="flex items-start justify-between gap-2 pb-3 border-b border-gray-100 last:border-0">
                    <div className="flex-1 min-w-0">
                      <div className="font-medium text-sm text-gray-900 truncate">{contact.name || 'Unnamed'}</div>
                      <div className="text-xs text-gray-500 truncate">{contact.email}</div>
                      {contact.title && (
                        <div className="text-xs text-gray-500 mt-1">{contact.title}</div>
                      )}
                    </div>
                    <div className="text-xs text-gray-400 flex-shrink-0">
                      {formatRelativeTime(contact.createdAt)}
                    </div>
                  </div>
                ))}
              </div>
            ) : (
              <div className="text-center text-gray-500 text-sm py-4">No recent contacts</div>
            )}
          </CardContent>
        </Card>
      </div>
    </div>
  );
}
