{{- define "learning-hub.name" -}}learning-hub{{- end }}
{{- define "learning-hub.fullname" -}}{{ .Release.Name }}{{- end }}
{{- define "learning-hub.labels" -}}
app.kubernetes.io/name: {{ include "learning-hub.name" . }}
app.kubernetes.io/instance: {{ .Release.Name }}
app.kubernetes.io/managed-by: {{ .Release.Service }}
helm.sh/chart: {{ printf "%s-%s" .Chart.Name .Chart.Version | quote }}
{{- end }}
{{- define "learning-hub.serviceAccountName" -}}
{{- if .Values.serviceAccount.create }}{{ include "learning-hub.fullname" . }}{{ else }}default{{ end -}}
{{- end }}
{{- define "learning-hub.podSecurityContext" -}}
runAsNonRoot: true
seccompProfile:
  type: RuntimeDefault
{{- end }}
{{- define "learning-hub.containerSecurityContext" -}}
allowPrivilegeEscalation: false
readOnlyRootFilesystem: true
capabilities:
  drop: ["ALL"]
{{- end }}
