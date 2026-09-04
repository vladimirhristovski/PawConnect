import { ParamMap, Params } from '@angular/router';

export type ParamType = 'string' | 'number' | 'boolean';
export type ParamSchema<T> = { [K in keyof T]?: ParamType };

export function readFiltersFromParams<T extends Record<string, any>>(
  paramMap: ParamMap,
  schema: ParamSchema<T>,
  base: T,
): T {
  const result = { ...base } as T;
  (Object.keys(schema) as (keyof T)[]).forEach((key) => {
    const raw = paramMap.get(key as string);
    if (raw === null || raw === '') return;
    const type = schema[key];
    if (type === 'number') {
      const parsed = Number(raw);
      if (!Number.isNaN(parsed)) result[key] = parsed as T[keyof T];
    } else if (type === 'boolean') {
      result[key] = (raw === 'true') as T[keyof T];
    } else {
      result[key] = raw as T[keyof T];
    }
  });
  return result;
}

export function filtersToQueryParams<T extends Record<string, any>>(
  filters: T,
  schema: ParamSchema<T>,
  defaults: Partial<T> = {},
): Params {
  const params: Params = {};
  (Object.keys(schema) as (keyof T)[]).forEach((key) => {
    const value = filters[key];
    if (value === undefined || value === null || value === '') return;
    if (typeof value === 'number' && Number.isNaN(value)) return;
    if (Object.prototype.hasOwnProperty.call(defaults, key) && value === defaults[key]) return;
    params[key as string] = value;
  });
  return params;
}

/**
 * True when `next` describes the same query string as `current` (the router's
 * live `snapshot.queryParams`). Values are compared as strings because the
 * router stores them that way.
 */
export function sameQueryParams(next: Params, current: Params): boolean {
  const nextKeys = Object.keys(next);
  const currentKeys = Object.keys(current);
  if (nextKeys.length !== currentKeys.length) return false;
  return nextKeys.every((key) => String(next[key]) === String(current[key]));
}
