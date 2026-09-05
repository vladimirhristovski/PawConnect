interface ProblemDetailBody {
  detail?: string;
  errors?: { field?: string; message?: string }[];
}

export function apiErrorMessage(err: unknown, fallback: string): string {
  const body = (err as { error?: ProblemDetailBody } | null)?.error;

  const fieldMessages = body?.errors
    ?.filter((e): e is { field?: string; message: string } => !!e.message)
    .map((e) => (e.field ? `${e.field}: ${e.message}` : e.message));
  if (fieldMessages && fieldMessages.length > 0) return fieldMessages.join('; ');

  const detail = body?.detail?.trim();
  if (detail) return detail;
  return fallback;
}
