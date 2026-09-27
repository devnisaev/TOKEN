import { Link } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import { Loader2 } from 'lucide-react';
import { api } from '@/lib/api';
import { useAuth } from '@/lib/auth';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { StatusBadge } from '@tokenrealty/shared-ui';

export function GovernancePage() {
  const { user } = useAuth();

  const { data, isLoading, error } = useQuery({
    queryKey: ['governance-proposals', user?.id],
    queryFn: () => api.listGovernanceProposals(user!.id),
    enabled: !!user?.id,
  });

  if (!user) return null;

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-3xl font-bold tracking-tight">Governance</h1>
        <p className="mt-1 text-muted-foreground">Open proposals for properties you hold</p>
      </div>

      {isLoading && (
        <div className="flex items-center text-muted-foreground">
          <Loader2 className="mr-2 h-5 w-5 animate-spin" />
          Loading proposals…
        </div>
      )}

      {error && (
        <div className="rounded-lg border border-destructive/30 bg-destructive/5 p-4 text-destructive">
          {error instanceof Error ? error.message : 'Failed to load proposals'}
        </div>
      )}

      {data && data.length === 0 && (
        <Card>
          <CardContent className="py-8 text-center text-muted-foreground">
            No open proposals for your holdings.
          </CardContent>
        </Card>
      )}

      <ul className="space-y-3">
        {data?.map((proposal) => (
          <li key={proposal.id}>
            <Link to={`/governance/${proposal.id}`}>
              <Card className="transition-colors hover:bg-muted/30">
                <CardHeader className="flex flex-row items-start justify-between space-y-0">
                  <div>
                    <CardTitle className="text-base">{proposal.title}</CardTitle>
                    <CardDescription className="line-clamp-2">{proposal.description}</CardDescription>
                  </div>
                  <StatusBadge status={proposal.status} />
                </CardHeader>
                <CardContent className="text-sm text-muted-foreground">
                  Quorum {proposal.quorumPct}% · For {proposal.votesFor} / Against {proposal.votesAgainst}
                  · Closes {new Date(proposal.closesAt).toLocaleString()}
                </CardContent>
              </Card>
            </Link>
          </li>
        ))}
      </ul>
    </div>
  );
}
