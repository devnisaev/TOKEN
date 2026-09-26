import { resolveApiBaseUrl } from '@tokenrealty/shared-api-client';
import type { PortfolioBffDetail } from '@/types/api';

const GRAPHQL_URL = `${resolveApiBaseUrl(import.meta.env.VITE_API_BASE_URL)}/graphql`;

type GraphQlPortfolioResponse = {
  data?: {
    investorPortfolio?: {
      balance: PortfolioBffDetail['balance'];
      recentDividends: PortfolioBffDetail['recentDividends'];
    };
  };
  errors?: Array<{ message: string }>;
};

export async function fetchPortfolioViaGraphql(
  investorId: string,
  accessToken: string | null,
): Promise<PortfolioBffDetail> {
  const response = await fetch(GRAPHQL_URL, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      ...(accessToken ? { Authorization: `Bearer ${accessToken}` } : {}),
    },
    body: JSON.stringify({
      query: `
        query InvestorPortfolio($investorId: ID!) {
          investorPortfolio(investorId: $investorId) {
            balance {
              investorId
              primaryWalletAddress
              fiatBalances { currency available held }
              tokenHoldings {
                contractId
                tokenSymbol
                walletAddress
                balance
                tokenPriceUsd
              }
            }
            recentDividends {
              id
              contractId
              amountUsd
              status
              paidAt
            }
          }
        }
      `,
      variables: { investorId },
    }),
  });

  if (!response.ok) {
    throw new Error(`GraphQL request failed (${response.status})`);
  }

  const body = (await response.json()) as GraphQlPortfolioResponse;
  if (body.errors?.length) {
    throw new Error(body.errors.map((e) => e.message).join('; '));
  }
  const portfolio = body.data?.investorPortfolio;
  if (!portfolio) {
    throw new Error('GraphQL portfolio response missing');
  }
  return portfolio;
}
