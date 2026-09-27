import { Link, useParams } from 'react-router-dom';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { ArrowLeft, Loader2 } from 'lucide-react';
import { api } from '@/lib/api';
import { useAuth } from '@/lib/auth';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';

export function GovernanceDetailPage() {
  const { proposalId } = useParams<{ proposalId: string }>();
  const { user } = useAuth();
  const queryClient = useQueryClient();

  const { data: proposals, isLoading, error } = useQuery({
    queryKey: ['governance-proposals', user?.id],
    queryFn: () => api.listGovernanceProposals(user!.id),
    enabled: !!user?.id,
  });

  const proposal = proposals?.find((p) => p.id === proposalId);

  const voteMutation = useMutation({
    mutationFn: (support: boolean) =>
      api.castGovernanceVote(proposalId!, user!.id, support),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['governance-proposals'] });
    },
  });

  if (!user) return null;

  if (isLoading) {
    return (
      <div className="flex items-center justify-center py-20 text-muted-foreground">
        <Loader2 className="mr-2 h-5 w-5 animate-spin" />
        Loading…
      </div>
    );
  }

  if (error || !proposal) {
    return (
      <div className="space-y-4">
        <Link to="/governance" className="inline-flex items-center text-sm text-muted-foreground">
          <ArrowLeft className="mr-1 h-4 w-4" /> Back
        </Link>
        <p className="text-destructive">Proposal not found or not available for your holdings.</p>
      </div>
    );
  }

  const totalVotes = proposal.votesFor + proposal.votesAgainst;
  const forPct = totalVotes === 0 ? 0 : Math.round((proposal.votesFor / totalVotes) * 100);

  return (
    <div className="mx-auto max-w-2xl space-y-6">
      <Link to="/governance" className="inline-flex items-center text-sm text-muted-foreground hover:text-foreground">
        <ArrowLeft className="mr-1 h-4 w-4" /> All proposals
      </Link>

      <Card>
        <CardHeader>
          <CardTitle>{proposal.title}</CardTitle>
        </CardHeader>
        <CardContent className="space-y-4">
          <p className="whitespace-pre-wrap text-sm">{proposal.description}</p>
          <div className="space-y-2">
            <div className="flex justify-between text-sm">
              <span>Vote progress</span>
              <span>{forPct}% for · quorum target {proposal.quorumPct}%</span>
            </div>
            <div className="h-2 overflow-hidden rounded-full bg-muted">
              <div className="h-full bg-primary" style={{ width: `${forPct}%` }} />
            </div>
          </div>
          {proposal.status === 'OPEN' && (
            <div className="flex gap-2">
              <Button disabled={voteMutation.isPending} onClick={() => voteMutation.mutate(true)}>
                Vote FOR
              </Button>
              <Button
                variant="outline"
                disabled={voteMutation.isPending}
                onClick={() => voteMutation.mutate(false)}
              >
                Vote AGAINST
              </Button>
            </div>
          )}
          {voteMutation.error && (
            <p className="text-sm text-destructive">
              {voteMutation.error instanceof Error ? voteMutation.error.message : 'Vote failed'}
            </p>
          )}
        </CardContent>
      </Card>
    </div>
  );
}
