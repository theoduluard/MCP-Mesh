export interface ToolContent {
  type: string;
  text: string;
}

export interface ToolCallResult {
  content: ToolContent[];
  isError?: boolean;
}

export interface CallToolHttpResponse {
  result: ToolCallResult;
}
