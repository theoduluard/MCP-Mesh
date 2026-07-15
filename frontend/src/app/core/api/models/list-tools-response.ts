export interface Tool {
  name: string;
  title?: string;
  description?: string;
  inputSchema?: unknown;
}

export interface ListToolsResult {
  tools: Tool[];
}

export interface ListToolsResponse {
  result: ListToolsResult;
}
