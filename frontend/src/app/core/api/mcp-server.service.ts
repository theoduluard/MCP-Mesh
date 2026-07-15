import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { CallToolHttpRequest } from './models/call-tool-http-request';
import { CallToolHttpResponse } from './models/call-tool-http-response';
import { ConnectServerResponse } from './models/connect-server-response';
import { ConnectServerRequest } from './models/connect-server-request';
import { ListToolsResponse } from './models/list-tools-response';
import { ServerListResponse } from './models/server-list-response';

const BASE_URL = 'http://localhost:8080/servers';

@Injectable({ providedIn: 'root' })
export class McpServerService {
  private readonly http = inject(HttpClient);

  connect(command: string[]): Observable<ConnectServerResponse> {
    const request: ConnectServerRequest = { command };
    return this.http.post<ConnectServerResponse>(`${BASE_URL}/connect`, request);
  }

  listServers(): Observable<ServerListResponse> {
    return this.http.get<ServerListResponse>(BASE_URL);
  }

  listTools(serverId: string): Observable<ListToolsResponse> {
    return this.http.get<ListToolsResponse>(`${BASE_URL}/${serverId}/tools`);
  }

  callTool(serverId: string, toolName: string, args: Record<string, unknown>): Observable<CallToolHttpResponse> {
    const request: CallToolHttpRequest = { arguments: args };
    return this.http.post<CallToolHttpResponse>(`${BASE_URL}/${serverId}/tools/${toolName}/call`, request);
  }

  disconnect(serverId: string): Observable<void> {
    return this.http.delete<void>(`${BASE_URL}/${serverId}`);
  }
}
