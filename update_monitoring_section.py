from pathlib import Path

p = Path('kiosk_보안_OWASP_2025_Top10.html')
s = p.read_text(encoding='utf-8')
start_marker = '            <!-- ===================== 향후 계획: EC2 + Grafana 모니터링 ===================== -->'
end_marker = '            <dialog class="image-lightbox"'
start = s.index(start_marker)
end = s.index(end_marker, start)

section = '''            <!-- ===================== 운영 모니터링 적용: Prometheus + Grafana ===================== -->
            <div class="section">
                <div class="section-header plan">
                    <span class="owasp-badge badge-plan">APPLIED</span>
                    <h2>운영 모니터링 적용 현황 (Actuator + Prometheus + Grafana)</h2>
                    <p class="subtitle">기존 임시 계획 내용을 현재 키오스크 배포 환경에 실제 적용한 구성 기준으로 갱신</p>
                </div>

                <div class="section-body">
                    <div class="item" id="monitoring-applied-1">
                        <div class="item-title">① 백엔드 메트릭 노출 — Spring Boot Actuator + Micrometer</div>

                        <div class="story">
                            <div class="story-label">✅ 적용 내용</div>
                            <p>
                                백엔드에 <strong>Spring Boot Actuator</strong>와 <strong>micrometer-registry-prometheus</strong>를 적용해
                                <code>/actuator/prometheus</code>에서 JVM, HTTP 요청, CPU, DB 커넥션 등의 운영 지표를
                                Prometheus 형식으로 노출하도록 구성했다.
                            </p>
                            <p>
                                모든 지표에는 <code>application: kiosk-backend</code> 태그를 붙여
                                Grafana에서 우리 백엔드만 선택해 조회할 수 있도록 했다.
                            </p>
                        </div>

                        <div class="code-block">
                            <div class="code-label"><span class="dot dot-r"></span><span class="dot dot-y"></span><span class="dot dot-g"></span>pom.xml — 실제 적용 의존성</div>
                            <pre><code class="language-markup">&lt;dependency&gt;
    &lt;groupId&gt;org.springframework.boot&lt;/groupId&gt;
    &lt;artifactId&gt;spring-boot-starter-actuator&lt;/artifactId&gt;
&lt;/dependency&gt;
&lt;dependency&gt;
    &lt;groupId&gt;io.micrometer&lt;/groupId&gt;
    &lt;artifactId&gt;micrometer-registry-prometheus&lt;/artifactId&gt;
&lt;/dependency&gt;</code></pre>
                        </div>

                        <div class="code-block">
                            <div class="code-label"><span class="dot dot-r"></span><span class="dot dot-y"></span><span class="dot dot-g"></span>application.yml — 실제 적용 설정</div>
                            <pre><code class="language-yaml">management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
  metrics:
    tags:
      application: kiosk-backend
  endpoint:
    health:
      show-details: always</code></pre>
                        </div>

                        <div class="improve">
                            <div class="improve-title">🔒 접근 범위</div>
                            <p>
                                백엔드 8080 포트는 보안그룹상 프론트·매니저 EC2에서만 접근 가능하고,
                                프론트 Nginx가 <code>/api</code>와 <code>/uploads</code>만 프록시하므로
                                <code>/actuator/prometheus</code>가 인터넷에 직접 노출되지 않도록 구성했다.
                            </p>
                        </div>
                    </div>

                    <div class="item" id="monitoring-applied-2">
                        <div class="item-title">② 매니저 EC2에 Prometheus + Grafana 실행</div>

                        <div class="story">
                            <div class="story-label">✅ 실제 아키텍처</div>
                            <p>
                                기존에 상시 사용하는 <strong>매니저 EC2</strong>에 Prometheus와 Grafana를 Docker Compose로 실행했다.
                                Prometheus는 백엔드 EC2의 프라이빗 IP를 대상으로 <strong>15초마다</strong>
                                <code>/actuator/prometheus</code>를 수집하고, Grafana는 같은 Docker 네트워크의
                                <code>http://prometheus:9090</code>을 데이터소스로 사용한다.
                            </p>
                        </div>

                        <div class="flow">
                            <div class="flow-item situation"><span class="step-label">백엔드 EC2</span>Actuator가 메트릭 노출</div>
                            <div class="flow-arrow">→</div>
                            <div class="flow-item apply"><span class="step-label">매니저 EC2</span>Prometheus가 15초마다 수집</div>
                            <div class="flow-arrow">→</div>
                            <div class="flow-item result"><span class="step-label">Grafana</span>Prometheus 지표 시각화</div>
                        </div>

                        <div class="code-block">
                            <div class="code-label"><span class="dot dot-r"></span><span class="dot dot-y"></span><span class="dot dot-g"></span>prometheus.yml — 적용 구조</div>
                            <pre><code class="language-yaml">global:
  scrape_interval: 15s

scrape_configs:
  - job_name: 'kiosk-backend'
    metrics_path: '/actuator/prometheus'
    static_configs:
      - targets: ['&lt;백엔드프라이빗IP&gt;:8080']</code></pre>
                        </div>

                        <div class="improve">
                            <div class="improve-title">🛡️ 포트 운영</div>
                            <p>
                                Grafana 화면용 매니저 EC2 <code>3000</code> 포트만 접속자 IP 기준으로 열고,
                                Prometheus <code>9090</code>은 외부에 개방하지 않는 구조로 구성했다.
                            </p>
                        </div>
                    </div>

                    <div class="item" id="monitoring-applied-3">
                        <div class="item-title">③ Grafana JVM 대시보드 연결 및 서비스 맞춤 패널 구성</div>

                        <div class="story">
                            <div class="story-label">📊 현재 적용 상태</div>
                            <p>
                                Grafana의 <strong>JVM (Micrometer) 템플릿(ID 4701)</strong>을 가져와 Prometheus와 연결했고,
                                대시보드 상단에서 <code>Application = kiosk-backend</code>와 백엔드 인스턴스가 인식되는 것까지 확인했다.
                            </p>
                            <p>
                                현재 기본 JVM 패널 일부가 <span class="hl-yellow">N/A / No data</span>로 표시되어
                                시간 범위·트래픽 발생 여부·메트릭 쿼리를 확인하면서 서비스 맞춤 패널을 추가하고 있다.
                                즉, 모니터링 스택과 데이터소스 연결은 구성되어 있고 대시보드 지표 표현을 점검하는 단계다.
                            </p>
                        </div>

                        <div class="story">
                            <div class="story-label" style="background:#4f46e5;">🔧 직접 추가하는 핵심 패널</div>
                            <p><strong>초당 HTTP 요청 수</strong></p>
                            <pre><code>sum(rate(http_server_requests_seconds_count{application="kiosk-backend"}[1m]))</code></pre>
                            <p><strong>5xx 에러 비율</strong></p>
                            <pre><code>sum(rate(http_server_requests_seconds_count{application="kiosk-backend",status=~"5.."}[5m]))
/
sum(rate(http_server_requests_seconds_count{application="kiosk-backend"}[5m]))</code></pre>
                            <p><strong>평균 응답시간</strong></p>
                            <pre><code>sum(rate(http_server_requests_seconds_sum{application="kiosk-backend"}[5m]))
/
sum(rate(http_server_requests_seconds_count{application="kiosk-backend"}[5m]))</code></pre>
                        </div>

                        <div class="summary">
                            <div class="summary-title">💡 현재 기준 핵심 정리</div>
                            <ul>
                                <li>백엔드에 Actuator + Micrometer를 적용해 Prometheus용 메트릭을 노출했다.</li>
                                <li>매니저 EC2에 Prometheus + Grafana를 실행하고 내부 네트워크로 연결했다.</li>
                                <li>Prometheus는 15초마다 백엔드 메트릭을 수집하도록 구성했다.</li>
                                <li>Grafana에 JVM (Micrometer) 대시보드를 Import했고 <code>kiosk-backend</code> 애플리케이션과 인스턴스가 선택되는 것을 확인했다.</li>
                                <li>HTTP 요청량·5xx 에러율·평균 응답시간 등의 서비스 맞춤 패널을 추가하면서 일부 No data 항목을 점검 중이다.</li>
                                <li>Discord 웹훅 알림은 아직 적용 완료 내용으로 적지 않고 후속 확장 항목으로 남겼다.</li>
                            </ul>
                        </div>
                    </div>
                </div>
            </div>


'''

p.write_text(s[:start] + section + s[end:], encoding='utf-8')
print('updated', p)
