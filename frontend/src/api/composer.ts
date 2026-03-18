export interface ComposerSummary {
  composerId: number;
  name: string;
}

export interface ComposerWorkSummary {
  workId: number;
  title: string;
}

export async function getComposers(): Promise<ComposerSummary[]> {
  const res = await fetch('/api/composers');
  if (!res.ok) throw new Error('Failed to fetch composers');
  return res.json();
}

export async function getComposerWorks(composerId: number): Promise<ComposerWorkSummary[]> {
  const res = await fetch(`/api/composers/${composerId}/works`);
  if (!res.ok) throw new Error('Failed to fetch composer works');
  return res.json();
}
