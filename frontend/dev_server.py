import http.server
import socketserver
import urllib.request
import urllib.error
import os
import sys

API_TARGET = os.environ.get('API_TARGET', 'http://localhost:8081')
PORT = int(os.environ.get('PORT', '3001'))
ROOT = os.path.dirname(os.path.abspath(__file__))
os.chdir(ROOT)


PROXY_PREFIXES = ('/api/', '/swagger', '/v3/api-docs', '/webjars/', '/actuator/', '/h2-console')


def is_proxy(path):
    return any(path == p.rstrip('/') or path.startswith(p) for p in PROXY_PREFIXES)


class Handler(http.server.SimpleHTTPRequestHandler):
    def do_GET(self):
        if is_proxy(self.path):
            return self._proxy('GET')
        return super().do_GET()

    def do_POST(self):
        if is_proxy(self.path):
            return self._proxy('POST')
        self.send_error(405)

    def do_DELETE(self):
        if is_proxy(self.path):
            return self._proxy('DELETE')
        self.send_error(405)

    def do_PUT(self):
        if is_proxy(self.path):
            return self._proxy('PUT')
        self.send_error(405)

    def do_OPTIONS(self):
        if is_proxy(self.path):
            return self._proxy('OPTIONS')
        self.send_response(204)
        self.end_headers()

    def _proxy(self, method):
        if self.path.startswith('/api/'):
            target = API_TARGET + self.path[len('/api'):]
        else:
            target = API_TARGET + self.path
        length = int(self.headers.get('Content-Length') or 0)
        body = self.rfile.read(length) if length else None
        req_headers = {}
        for h in ('Content-Type', 'Accept', 'Authorization'):
            v = self.headers.get(h)
            if v:
                req_headers[h] = v
        req = urllib.request.Request(target, data=body, method=method, headers=req_headers)
        try:
            with urllib.request.urlopen(req, timeout=15) as resp:
                self.send_response(resp.status)
                for k, v in resp.getheaders():
                    if k.lower() not in ('transfer-encoding', 'connection', 'content-encoding'):
                        self.send_header(k, v)
                self.end_headers()
                self.wfile.write(resp.read())
        except urllib.error.HTTPError as e:
            self.send_response(e.code)
            body = e.read()
            for k, v in e.headers.items():
                if k.lower() not in ('transfer-encoding', 'connection', 'content-encoding'):
                    self.send_header(k, v)
            self.end_headers()
            self.wfile.write(body)
        except urllib.error.URLError as e:
            self.send_response(502)
            self.send_header('Content-Type', 'application/json')
            self.end_headers()
            self.wfile.write(('{"status":502,"message":"upstream unreachable: ' + str(e) + '"}').encode('utf-8'))

    def log_message(self, format, *args):
        sys.stderr.write('[%s] %s\n' % (self.log_date_time_string(), format % args))


class ReuseTCPServer(socketserver.ThreadingMixIn, http.server.HTTPServer):
    allow_reuse_address = True
    daemon_threads = True


print('Frontend  : http://localhost:%d' % PORT)
print('API proxy : %s -> /api' % API_TARGET)
ReuseTCPServer(('0.0.0.0', PORT), Handler).serve_forever()
