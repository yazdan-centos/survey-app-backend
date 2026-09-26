$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.IO.Compression
Add-Type -AssemblyName System.IO.Compression.FileSystem
$root = Split-Path $PSScriptRoot -Parent
$questions = (Get-Content -LiteralPath (Join-Path $root 'src/main/resources/surveyQuestions.json') -Raw -Encoding UTF8 | ConvertFrom-Json).managers
$output = Join-Path $PSScriptRoot 'world-class-managers-questions.xlsx'
function EscapeXml($value) { [System.Security.SecurityElement]::Escape([string]$value) }
function Cell($column, $row, $value, $style = 0) {
    $address = "$column$row"
    if ($value -is [int]) { return "<c r=`"$address`" s=`"$style`"><v>$value</v></c>" }
    return "<c r=`"$address`" s=`"$style`" t=`"inlineStr`"><is><t xml:space=`"preserve`">$(EscapeXml $value)</t></is></c>"
}
$headers = @('code','text','role','level_title','level_score','level_order','dimension_key','dimension_label','criterion','display_order','level_description')
$rows = [System.Text.StringBuilder]::new()
[void]$rows.Append('<row r="1" ht="28" customHeight="1">')
for ($i=0; $i -lt $headers.Count; $i++) { [void]$rows.Append((Cell ([string][char](65+$i)) 1 $headers[$i] 1)) }
[void]$rows.Append('</row>')
$row = 2
$order = 0
foreach ($question in $questions) {
    $order++
    for ($level=1; $level -le $question.levels.Count; $level++) {
        $values = @($question.code, $question.criterion, 'MANAGERS', $level, $level, $level, $question.dimensionKey, $question.dimensionLabel, $question.criterion, $order, $question.levels[$level-1])
        [void]$rows.Append("<row r=`"$row`" ht=`"100`" customHeight=`"1`">")
        for ($i=0; $i -lt $values.Count; $i++) { [void]$rows.Append((Cell ([string][char](65+$i)) $row $values[$i] 2)) }
        [void]$rows.Append('</row>')
        $row++
    }
}
$notes = @(
    'World-class survey - Managers / پرسشنامه کلاس جهانی ویژه مدیران',
    'Source: src/main/resources/surveyQuestions.json, managers section. Original Persian content preserved.',
    '25 questions, four maturity levels per question, 100 data rows. Role: MANAGERS. Scores: 1 through 4.',
    'Questions is the first worksheet. The first six columns match the existing import template; columns G-K preserve required survey metadata and assessment descriptions.',
    'Backend limitation: QuestionExcelService currently reads only A-F and omits required criterion, dimension, levelNumber and description fields. The importer must be corrected before successful import.',
    'level_title is numeric by design. Full Persian assessment text is in level_description.',
    'After correcting the importer, use POST /api/questions/import with multipart fields surveyId and file.',
    'Import into a survey without these manager codes; the current importer inserts records and does not update existing questions.'
)
$noteRows = [System.Text.StringBuilder]::new()
for ($i=0; $i -lt $notes.Count; $i++) { [void]$noteRows.Append("<row r=`"$($i+1)`" ht=`"48`" customHeight=`"1`">$(Cell 'A' ($i+1) $notes[$i] 2)</row>") }
$ns = 'http://schemas.openxmlformats.org/spreadsheetml/2006/main'
$parts = @{
    '[Content_Types].xml' = '<?xml version="1.0" encoding="UTF-8"?><Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types"><Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/><Default Extension="xml" ContentType="application/xml"/><Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/><Override PartName="/xl/styles.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml"/><Override PartName="/xl/worksheets/sheet1.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/><Override PartName="/xl/worksheets/sheet2.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/></Types>'
    '_rels/.rels' = '<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships"><Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/></Relationships>'
    'xl/workbook.xml' = "<workbook xmlns=`"$ns`" xmlns:r=`"http://schemas.openxmlformats.org/officeDocument/2006/relationships`"><sheets><sheet name=`"Questions`" sheetId=`"1`" r:id=`"rId1`"/><sheet name=`"Read Me`" sheetId=`"2`" r:id=`"rId2`"/></sheets></workbook>"
    'xl/_rels/workbook.xml.rels' = '<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships"><Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet1.xml"/><Relationship Id="rId2" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet2.xml"/><Relationship Id="rId3" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles" Target="styles.xml"/></Relationships>'
    'xl/styles.xml' = "<styleSheet xmlns=`"$ns`"><fonts count=`"2`"><font><sz val=`"11`"/><name val=`"Arial`"/></font><font><b/><color rgb=`"FFFFFFFF`"/><sz val=`"11`"/><name val=`"Arial`"/></font></fonts><fills count=`"3`"><fill><patternFill patternType=`"none`"/></fill><fill><patternFill patternType=`"gray125`"/></fill><fill><patternFill patternType=`"solid`"><fgColor rgb=`"FF17365D`"/><bgColor indexed=`"64`"/></patternFill></fill></fills><borders count=`"1`"><border/></borders><cellStyleXfs count=`"1`"><xf numFmtId=`"0`" fontId=`"0`" fillId=`"0`" borderId=`"0`"/></cellStyleXfs><cellXfs count=`"3`"><xf numFmtId=`"0`" fontId=`"0`" fillId=`"0`" borderId=`"0`" xfId=`"0`"/><xf numFmtId=`"0`" fontId=`"1`" fillId=`"2`" borderId=`"0`" xfId=`"0`" applyAlignment=`"1`"><alignment vertical=`"center`"/></xf><xf numFmtId=`"0`" fontId=`"0`" fillId=`"0`" borderId=`"0`" xfId=`"0`" applyAlignment=`"1`"><alignment vertical=`"top`" wrapText=`"1`"/></xf></cellXfs><cellStyles count=`"1`"><cellStyle name=`"Normal`" xfId=`"0`" builtinId=`"0`"/></cellStyles></styleSheet>"
    'xl/worksheets/sheet1.xml' = "<worksheet xmlns=`"$ns`"><dimension ref=`"A1:K$($row-1)`"/><sheetViews><sheetView workbookViewId=`"0`" rightToLeft=`"1`"><pane ySplit=`"1`" topLeftCell=`"A2`" activePane=`"bottomLeft`" state=`"frozen`"/></sheetView></sheetViews><cols><col min=`"1`" max=`"1`" width=`"12`" customWidth=`"1`"/><col min=`"2`" max=`"2`" width=`"40`" customWidth=`"1`"/><col min=`"3`" max=`"6`" width=`"16`" customWidth=`"1`"/><col min=`"7`" max=`"10`" width=`"28`" customWidth=`"1`"/><col min=`"11`" max=`"11`" width=`"110`" customWidth=`"1`"/></cols><sheetData>$rows</sheetData><autoFilter ref=`"A1:K$($row-1)`"/></worksheet>"
    'xl/worksheets/sheet2.xml' = "<worksheet xmlns=`"$ns`"><cols><col min=`"1`" max=`"1`" width=`"130`" customWidth=`"1`"/></cols><sheetData>$noteRows</sheetData></worksheet>"
}
$stream = [System.IO.File]::Open($output, [System.IO.FileMode]::Create)
$zip = [System.IO.Compression.ZipArchive]::new($stream, [System.IO.Compression.ZipArchiveMode]::Create)
try {
    foreach ($name in $parts.Keys) {
        $entry = $zip.CreateEntry($name)
        $writer = [System.IO.StreamWriter]::new($entry.Open(), [System.Text.UTF8Encoding]::new($false))
        try { $writer.Write($parts[$name]) } finally { $writer.Dispose() }
    }
} finally { $zip.Dispose(); $stream.Dispose() }
$check = [System.IO.Compression.ZipFile]::OpenRead($output)
try {
    foreach ($entry in $check.Entries) {
        $reader = [System.IO.StreamReader]::new($entry.Open())
        try { $xml = [xml]$reader.ReadToEnd() } finally { $reader.Dispose() }
        if ($entry.FullName -eq 'xl/worksheets/sheet1.xml') {
            $actualRows = @($xml.worksheet.sheetData.row)
            if ($actualRows.Count -ne 101) { throw 'Unexpected question row count' }
            foreach ($dataRow in $actualRows[1..100]) {
                if (@($dataRow.c).Count -ne 11) { throw 'Unexpected cell count' }
            }
        }
    }
} finally { $check.Dispose() }
Write-Output "Created and validated $output (25 questions, 100 assessment rows)."
