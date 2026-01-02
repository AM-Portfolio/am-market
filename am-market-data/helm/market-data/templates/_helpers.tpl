{{/*
Expand the name of the chart.
*/}}
{{- define "am-market-data.name" -}}
{{- default .Chart.Name .Values.nameOverride | trunc 63 | trimSuffix "-" }}
{{- end }}

{{/*
Create a default fully qualified app name.
*/}}
{{- define "am-market-data.fullname" -}}
{{- default .Chart.Name .Values.fullnameOverride | trunc 63 | trimSuffix "-" }}
{{- end }}

{{/*
Common labels
*/}}
{{- define "am-market-data.labels" -}}
app.kubernetes.io/name: {{ include "am-market-data.name" . }}
app.kubernetes.io/instance: {{ .Release.Name }}
app.kubernetes.io/version: {{ .Chart.AppVersion | quote }}
app.kubernetes.io/managed-by: {{ .Release.Service }}
{{- end }}

{{/*
Selector labels
*/}}
{{- define "am-market-data.selectorLabels" -}}
app.kubernetes.io/name: {{ include "am-market-data.name" . }}
app: am-market-data
{{- end }}

{{/*
Infrastructure service names
*/}}
{{- define "am-market-data.postgresql.fullname" -}}
{{- .Values.postgresql.fullname }}
{{- end }}

{{- define "am-market-data.influxdb.fullname" -}}
{{- .Values.influxdb.url }}
{{- end }}

{{- define "am-market-data.kafka.fullname" -}}
{{- .Values.kafka.bootstrapServers }}
{{- end }}

{{- define "am-market-data.zookeeper.fullname" -}}
{{- .Values.kafka.zookeeper.connect }}
{{- end }}
