export function buildUniqueId(): string {
  const dateString = Date.now().toString(36)
  const randomness = Math.random().toString(36).substring(2)
  return dateString + randomness
}
