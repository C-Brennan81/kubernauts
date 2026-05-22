{{/*
Common labels
*/}}
{{- define "kubernauts.labels" -}}
app.kubernetes.io/name: kubernauts
app.kubernetes.io/instance: {{ .Release.Name }}
app.kubernetes.io/version: {{ .Chart.AppVersion }}
app.kubernetes.io/managed-by: {{ .Release.Service }}
{{- end }}
