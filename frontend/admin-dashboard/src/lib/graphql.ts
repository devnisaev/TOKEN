import { resolveApiBaseUrl } from '@tokenrealty/shared-api-client';
import type { MaintenanceTicket } from '@/types/api';

const GRAPHQL_URL = `${resolveApiBaseUrl(import.meta.env.VITE_API_BASE_URL)}/graphql`;

type GraphQlMaintenanceResponse = {
  data?: {
    adminMaintenanceQueue?: MaintenanceTicket[];
  };
  errors?: Array<{ message: string }>;
};

export async function fetchMaintenanceQueueViaGraphql(
  accessToken: string | null,
): Promise<MaintenanceTicket[]> {
  const response = await fetch(GRAPHQL_URL, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      ...(accessToken ? { Authorization: `Bearer ${accessToken}` } : {}),
    },
    body: JSON.stringify({
      query: `
        query AdminMaintenanceQueue {
          adminMaintenanceQueue {
            id
            leaseId
            title
            description
            status
            createdAt
          }
        }
      `,
    }),
  });

  if (!response.ok) {
    throw new Error(`GraphQL request failed (${response.status})`);
  }

  const body = (await response.json()) as GraphQlMaintenanceResponse;
  if (body.errors?.length) {
    throw new Error(body.errors.map((e) => e.message).join('; '));
  }
  return body.data?.adminMaintenanceQueue ?? [];
}
