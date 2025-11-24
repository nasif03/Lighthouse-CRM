import { useEffect, useMemo, useState } from 'react';
import Card, { CardContent, CardHeader } from '../components/ui/Card';
import Button from '../components/ui/Button';
import { Table, TBody, TD, TH, THead, TR } from '../components/ui/Table';
import { useAuthStore } from '../store/authStore';
import { FirefliesTranscript, useFirefliesStore } from '../store/firefliesStore';

function formatDate(dateString?: string | null) {
  if (!dateString) return '—';
  const date = new Date(dateString);
  if (isNaN(date.getTime())) return dateString;
  return date.toLocaleString(undefined, {
    month: 'short',
    day: 'numeric',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  });
}

function truncate(text?: string | null, length = 140) {
  if (!text) return '—';
  return text.length > length ? `${text.slice(0, length)}…` : text;
}

export default function Fireflies() {
  const { token } = useAuthStore();
  const {
    transcripts,
    isLoading,
    isSyncing,
    error,
    lastFetchedAt,
    lastSyncedAt,
    fetchTranscripts,
    syncTranscripts,
    clearError,
  } = useFirefliesStore();

  const [selectedTranscriptId, setSelectedTranscriptId] = useState<string | null>(null);
  const [syncMessage, setSyncMessage] = useState<string | null>(null);

  const selectedTranscript: FirefliesTranscript | null = useMemo(() => {
    return transcripts.find((t) => t.id === selectedTranscriptId) || null;
  }, [selectedTranscriptId, transcripts]);

  useEffect(() => {
    if (token) {
      fetchTranscripts(token).catch((err) =>
        console.error('Fireflies initial fetch failed:', err)
      );
    }
  }, [token, fetchTranscripts]);

  const handleRefresh = async () => {
    if (!token) return;
    await fetchTranscripts(token);
    setSyncMessage('Transcript list refreshed.');
    setTimeout(() => setSyncMessage(null), 3000);
  };

  const handleSync = async () => {
    if (!token) return;
    const saved = await syncTranscripts(token);
    if (saved > 0) {
      setSyncMessage(`Synced ${saved} transcript${saved === 1 ? '' : 's'} from Fireflies.`);
    } else {
      setSyncMessage('No new transcripts were synced.');
    }
    setTimeout(() => setSyncMessage(null), 5000);
  };

  return (
    <div className="space-y-6">
      <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
        <Card>
          <CardHeader>Fireflies AI transcripts</CardHeader>
          <CardContent className="space-y-3">
            <p className="text-sm text-gray-600">
              Pull meeting summaries from Fireflies AI into Lighthouse. Use this panel to sync the
              latest conversations and review AI-generated notes.
            </p>
            <div className="flex flex-wrap gap-3">
              <Button
                onClick={handleRefresh}
                disabled={isLoading || !token}
                title={token ? 'Reload the latest transcripts' : 'Login required'}
              >
                {isLoading ? 'Refreshing…' : 'Refresh list'}
              </Button>
              <Button
                variant="secondary"
                onClick={handleSync}
                disabled={isSyncing || !token}
                title={token ? 'Fetch from Fireflies API and save to CRM' : 'Login required'}
              >
                {isSyncing ? 'Syncing…' : 'Sync from Fireflies'}
              </Button>
            </div>
            <div className="text-xs text-gray-500 space-y-1">
              {lastFetchedAt && <div>Last refreshed: {formatDate(lastFetchedAt)}</div>}
              {lastSyncedAt && <div>Last synced: {formatDate(lastSyncedAt)}</div>}
              {!token && <div className="text-red-500">Sign in to load transcripts.</div>}
              {syncMessage && <div className="text-brand-600">{syncMessage}</div>}
              {error && (
                <div className="text-red-600 flex items-center gap-2">
                  <span>{error}</span>
                  <button
                    type="button"
                    className="text-xs underline"
                    onClick={() => clearError()}
                  >
                    Dismiss
                  </button>
                </div>
              )}
            </div>
          </CardContent>
        </Card>

        <Card>
          <CardHeader>Transcript details</CardHeader>
          <CardContent className="space-y-3">
            {selectedTranscript ? (
              <>
                <div>
                  <div className="text-sm font-semibold text-gray-900">
                    {selectedTranscript.title || 'Untitled meeting'}
                  </div>
                  <div className="text-xs text-gray-500">{formatDate(selectedTranscript.date)}</div>
                </div>
                <div>
                  <div className="text-xs uppercase tracking-wide text-gray-500 mb-1">Overview</div>
                  <p className="text-sm text-gray-700 whitespace-pre-line">
                    {selectedTranscript.summary?.overview || 'No overview provided.'}
                  </p>
                </div>
                <div>
                  <div className="text-xs uppercase tracking-wide text-gray-500 mb-1">
                    Key points
                  </div>
                  <p className="text-sm text-gray-700 whitespace-pre-line">
                    {selectedTranscript.summary?.short_summary || 'No highlights provided.'}
                  </p>
                </div>
                {selectedTranscript.transcript_url && (
                  <Button
                    size="sm"
                    variant="secondary"
                    onClick={() => window.open(selectedTranscript.transcript_url!, '_blank')}
                  >
                    View full transcript
                  </Button>
                )}
              </>
            ) : (
              <p className="text-sm text-gray-600">
                Select a transcript from the list to see AI-generated summaries and open the full
                Fireflies document.
              </p>
            )}
          </CardContent>
        </Card>
      </div>

      <Card>
        <CardHeader>Recent transcripts</CardHeader>
        <CardContent>
          {isLoading && transcripts.length === 0 ? (
            <div className="py-16 text-center text-gray-500 text-sm">Loading transcripts…</div>
          ) : transcripts.length === 0 ? (
            <div className="py-16 text-center text-gray-500 text-sm">
              No transcripts found. Use “Sync from Fireflies” to pull your recent meetings.
            </div>
          ) : (
            <Table className="w-full text-sm">
              <THead>
                <TR className="bg-gray-50 text-left text-gray-600 text-xs uppercase tracking-wide">
                  <TH>Title</TH>
                  <TH>Date</TH>
                  <TH>Summary</TH>
                  <TH>Status</TH>
                </TR>
              </THead>
              <TBody>
                {transcripts.map((transcript) => (
                  <TR
                    key={transcript.id}
                    className={`cursor-pointer ${selectedTranscriptId === transcript.id ? 'bg-brand-50' : 'hover:bg-gray-50'}`}
                    onClick={() => setSelectedTranscriptId(transcript.id)}
                  >
                    <TD className="font-medium text-gray-900">{transcript.title || 'Untitled meeting'}</TD>
                    <TD className="text-gray-600">{formatDate(transcript.date)}</TD>
                    <TD className="text-gray-600">{truncate(transcript.summary?.overview)}</TD>
                    <TD>
                      {transcript.transcript_url ? (
                        <Button
                          size="sm"
                          variant="ghost"
                          onClick={(e) => {
                            e.stopPropagation();
                            window.open(transcript.transcript_url!, '_blank');
                          }}
                        >
                          Open
                        </Button>
                      ) : (
                        <span className="text-xs text-gray-500">No link</span>
                      )}
                    </TD>
                  </TR>
                ))}
              </TBody>
            </Table>
          )}
        </CardContent>
      </Card>
    </div>
  );
}

