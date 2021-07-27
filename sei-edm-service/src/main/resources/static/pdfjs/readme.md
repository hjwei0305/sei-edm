
PDF.js 打印模糊  web/viewer.js 中
var PRINT_RESOLUTION = 600;//打印精度，原来默认的150，越大越清晰，但初始化打印时也越慢。

PDF.js 不显示电子签章
build/pdf.worker.js中注释掉this.setFlags(AnnotationFlag.HIDDEN);