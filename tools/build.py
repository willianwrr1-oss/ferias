# Monta os arquivos do app a partir de head.html + body.html.
#  - www/index.html            -> app de verdade (começa vazio)  [vai para o APK]
#  - tools/simulacao.html      -> versão com dados de exemplo (usada na simulação)
import pathlib
raiz = pathlib.Path(__file__).resolve().parent
head = (raiz / "head.html").read_text(encoding="utf-8")
body = (raiz / "body.html").read_text(encoding="utf-8")

CDN = "https://cdnjs.cloudflare.com/ajax/libs/jspdf/2.5.1/jspdf.umd.min.js"

def doc(demo):
    corpo = body.replace("__DEMO__", "true" if demo else "false").replace("__JSPDF__", CDN if demo else "vendor/jspdf.umd.min.js")
    return ('<!doctype html>\n<html lang="pt-BR">\n<head>\n<meta charset="utf-8">\n'
            '<meta name="viewport" content="width=device-width, initial-scale=1, viewport-fit=cover">\n'
            '<meta name="theme-color" content="#0B6B75">\n<meta name="color-scheme" content="light dark">\n'
            + head + '\n</head>\n<body>\n' + corpo + '\n</body>\n</html>\n')

(raiz.parent / "www").mkdir(exist_ok=True)
(raiz.parent / "www" / "index.html").write_text(doc(False), encoding="utf-8")
(raiz / "simulacao.html").write_text(head + "\n" + body.replace("__DEMO__", "true").replace("__JSPDF__", CDN), encoding="utf-8")
print("ok")
