# Gestão de Férias

Aplicativo Android (Android 13 ou superior) para cadastrar e acompanhar as férias dos colaboradores.
Os dados ficam salvos no próprio celular. Não precisa de internet para usar.

## O que o app tem

- **Resumo**: quem está de férias hoje, saídas dos próximos 60 dias e cadastros aguardando aprovação (com botão Aprovar).
- **Cadastros**: lista com busca por nome ou matrícula e filtros por setor e situação. Toque num cartão para editar ou excluir.
- **Calendário**: mês com cor de destaque quando é o mês atual, dia de hoje marcado e uma bolinha vermelha em todo dia que tem férias cadastradas. Respeita os filtros. Toque num dia para ver quem está de férias nele. Deslize para trocar de mês.
- **Exportar**: gera um relatório em PDF (A4, retrato) com indicadores, resumo por setor e tabela de cadastros com paginação e rodapé. Dá para exportar tudo ou só um período específico (entram os cadastros que têm algum dia dentro do período), com filtro opcional por setor. Ao gerar, o Android abre a janela de compartilhar para salvar em Arquivos ou Drive, ou enviar por WhatsApp e e-mail.
- **Novo cadastro**: nome, matrícula, setor, início, fim, situação (aprovada ou pendente) e observações. Mostra os dias corridos e a data de retorno, e avisa quando outro colaborador do mesmo setor tem férias no mesmo período.

## Como gerar o APK (só pelo site do GitHub)

1. Crie um repositório novo no GitHub.
2. Em **Add file > Upload files**, envie todo o conteúdo desta pasta, incluindo a pasta oculta `.github`. Se o GitHub não aceitar a pasta `.github`, use **Add file > Create new file**, digite o nome `.github/workflows/build-apk.yml` e cole o conteúdo do arquivo de mesmo nome.
3. Abra a aba **Actions**. O build começa sozinho a cada envio. Para rodar na hora, escolha **Gerar APK > Run workflow**.
4. Espere ficar verde (uns 5 a 8 minutos).
5. Baixe o `Gestao-de-Ferias.apk`. Há dois caminhos:
   - Em **Actions**, abra a execução que ficou verde. No **Summary** aparece o link "Gestao-de-Ferias.apk": toque nele e o arquivo .apk baixa direto.
   - Em **Releases** (lado direito da página inicial do repositório), toque em **Gestao-de-Ferias.apk** na lista **Assets**. Não use "Source code (zip)" nem "Source code (tar.gz)": esses são só o código do projeto (o `.tar.gz` é o arquivo que não instala).
   O item *Gestao-de-Ferias-apk* no fim da execução em Actions baixa um `.zip` com o APK dentro; também serve, mas precisa abrir o zip.
6. No celular, abra o arquivo e permita a instalação de apps desta origem, se o Android pedir.

O APK gerado é de teste (assinado com a chave de debug). Instala normalmente. Atualizar por cima de uma versão anterior só funciona se ela veio do mesmo repositório.

## Estrutura

- `www/index.html`: o aplicativo (telas, calendário e regras). `www/vendor/jspdf.umd.min.js` é o gerador de PDF (jsPDF 2.5.1), embutido para funcionar sem internet.
- `android-overrides/res/`: ícone, ícone adaptativo e tela de abertura, copiados para o projeto Android durante o build.
- `capacitor.config.json` e `package.json`: configuração do empacotamento (inclui os plugins Filesystem e Share, usados para entregar o PDF).
- `.github/workflows/build-apk.yml`: receita que compila o APK com Android 13 (API 33) como mínimo.
- `tools/`: arquivos-fonte e script que gerou o ícone e o `www/index.html`. Não são necessários para o build.
