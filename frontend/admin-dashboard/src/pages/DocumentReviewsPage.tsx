import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { FileCheck, Loader2 } from 'lucide-react';
import { api } from '@/lib/api';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';

export function DocumentReviewsPage() {
  const queryClient = useQueryClient();

  const { data, isLoading, error } = useQuery({
    queryKey: ['document-reviews', 'pending'],
    queryFn: () => api.listPendingDocumentReviews(),
  });

  const verifyMutation = useMutation({
    mutationFn: (documentId: string) => api.verifyDocumentReview(documentId),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['document-reviews'] }),
  });

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-3xl font-bold tracking-tight">Document reviews</h1>
        <p className="text-muted-foreground">Data room documents awaiting compliance approval</p>
      </div>

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
        {data?.map((doc) => (
          <li key={doc.id}>
            <Card>
              <CardHeader className="flex flex-row items-start justify-between space-y-0">
                <div>
                  <CardTitle className="flex items-center gap-2 text-base">
                    <FileCheck className="h-4 w-4" />
                    {doc.documentType}
                  </CardTitle>
                  <p className="font-mono text-xs text-muted-foreground">{doc.ipfsCid}</p>
                </div>
                <span className="rounded-md bg-secondary px-2 py-1 text-xs font-medium">{doc.status}</span>
              </CardHeader>
              <CardContent className="flex items-center justify-between text-sm">
                <span className="text-muted-foreground">Building {doc.buildingId.slice(0, 8)}…</span>
                {!doc.isVerified && (
                  <Button
                    size="sm"
                    disabled={verifyMutation.isPending}
                    onClick={() => verifyMutation.mutate(doc.documentId)}
                  >
                    Approve document
                  </Button>
                )}
              </CardContent>
            </Card>
          </li>
        ))}
      </ul>

      {data?.length === 0 && !isLoading && (
        <p className="text-muted-foreground">No documents pending review.</p>
      )}
    </div>
  );
}
