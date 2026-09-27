import { FormEvent, useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Loader2 } from 'lucide-react';
import { api } from '@/lib/api';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { StatusBadge } from '@tokenrealty/shared-ui';

export function GovernanceAdminPage() {
  const queryClient = useQueryClient();
  const [flatId, setFlatId] = useState('');
  const [title, setTitle] = useState('');
  const [description, setDescription] = useState('');
  const [quorumPct, setQuorumPct] = useState('51');
  const [closesAt, setClosesAt] = useState('');

  const { data, isLoading, error } = useQuery({
    queryKey: ['admin-governance'],
    queryFn: () => api.listGovernanceProposals(),
  });

  const createMutation = useMutation({
    mutationFn: () =>
      api.createGovernanceProposal({
        flatId,
        title,
        description,
        quorumPct: parseFloat(quorumPct),
        closesAt: new Date(closesAt).toISOString(),
      }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin-governance'] });
      setTitle('');
      setDescription('');
    },
  });

  const closeMutation = useMutation({
    mutationFn: (id: string) => api.closeGovernanceProposal(id),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['admin-governance'] }),
  });

  function onCreate(e: FormEvent) {
    e.preventDefault();
    createMutation.mutate();
  }

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-3xl font-bold tracking-tight">Governance</h1>
        <p className="text-muted-foreground">Create and close token-holder proposals</p>
      </div>

      <Card>
        <CardHeader>
          <CardTitle className="text-base">New proposal</CardTitle>
        </CardHeader>
        <CardContent>
          <form onSubmit={onCreate} className="grid gap-4 sm:grid-cols-2">
            <div className="space-y-2">
              <Label htmlFor="flatId">Flat ID</Label>
              <Input id="flatId" value={flatId} onChange={(e) => setFlatId(e.target.value)} required />
            </div>
            <div className="space-y-2">
              <Label htmlFor="quorum">Quorum %</Label>
              <Input id="quorum" type="number" min="0.01" max="100" value={quorumPct} onChange={(e) => setQuorumPct(e.target.value)} required />
            </div>
            <div className="space-y-2 sm:col-span-2">
              <Label htmlFor="title">Title</Label>
              <Input id="title" value={title} onChange={(e) => setTitle(e.target.value)} required />
            </div>
            <div className="space-y-2 sm:col-span-2">
              <Label htmlFor="description">Description</Label>
              <Input id="description" value={description} onChange={(e) => setDescription(e.target.value)} required />
            </div>
            <div className="space-y-2">
              <Label htmlFor="closesAt">Closes at</Label>
              <Input id="closesAt" type="datetime-local" value={closesAt} onChange={(e) => setClosesAt(e.target.value)} required />
            </div>
            <div className="flex items-end">
              <Button type="submit" disabled={createMutation.isPending}>Create</Button>
            </div>
          </form>
        </CardContent>
      </Card>

      {isLoading && (
        <div className="flex items-center text-muted-foreground">
          <Loader2 className="mr-2 h-5 w-5 animate-spin" />
          Loading…
        </div>
      )}

      {error && (
        <p className="text-destructive">{error instanceof Error ? error.message : 'Failed to load'}</p>
      )}

      <ul className="space-y-3">
        {data?.content.map((proposal) => (
          <li key={proposal.id}>
            <Card>
              <CardHeader className="flex flex-row items-start justify-between space-y-0">
                <div>
                  <CardTitle className="text-base">{proposal.title}</CardTitle>
                  <p className="text-xs text-muted-foreground">Flat {proposal.flatId}</p>
                </div>
                <StatusBadge status={proposal.status} />
              </CardHeader>
              <CardContent className="flex items-center justify-between">
                <span className="text-sm text-muted-foreground">
                  For {proposal.votesFor} / Against {proposal.votesAgainst}
                </span>
                {proposal.status === 'OPEN' && (
                  <Button size="sm" variant="outline" disabled={closeMutation.isPending} onClick={() => closeMutation.mutate(proposal.id)}>
                    Close
                  </Button>
                )}
              </CardContent>
            </Card>
          </li>
        ))}
      </ul>
    </div>
  );
}
