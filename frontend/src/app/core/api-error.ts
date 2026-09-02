interface ProblemDetailBody {
  detail?: string;
  errors?: { field?: string; message?: string }[];
}

export function apiErrorMessage(err: unknown, fallback: string): string {
  const body = (err as { error?: ProblemDetailBody } | null)?.error;

  const fieldMessages = body?.errors
    ?.map((e) => e.message)
    .filter((m): m is string => !!m);
  if (fieldMessages && fieldMessages.length > 0) return fieldMessages.join(' ');

  if (body?.detail) return body.detail;
  return fallback;
}
