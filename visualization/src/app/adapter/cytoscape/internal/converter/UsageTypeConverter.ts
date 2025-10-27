const typeOfUsageMapping: Record<string, string> = {
  usage: "Uses",
  inheritance: "Inherits",
  implementation: "Implements",
  constant_access: "Constant access",
  return_value: "Return value",
  instantiation: "Instantiates",
  argument: "Argument"
};

export function convertTypeOfUsage(rawValue: string): string {
  const usageTypes = rawValue.split(",").filter(s=> s.trim() !== "usage" )

  const mappedVerbs = usageTypes.map(type => {
    const trimmedType = type.trim();
    return typeOfUsageMapping[trimmedType];
  });
  return mappedVerbs.join(" / ");
}
