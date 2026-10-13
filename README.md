# Gestão de Férias

Aplicativo Android (Android 13 ou superior) para cadastrar e acompanhar as férias dos colaboradores.
Os dados ficam salvos no próprio celular. Não precisa de internet para usar.

## O que o app tem

- **Resumo**: quem está de férias hoje, saídas dos próximos 60 dias e cadastros aguardando aprovação (com botão Aprovar).
- **Cadastros**: lista com busca por nome ou matrícula e filtros por setor e situação. Toque num cartão para editar ou excluir.
- **Calendário**: mês com cor de destaque quando é o mês atual, dia de hoje marcado e uma bolinha vermelha em todo dia que tem férias cadastradas. Respeita os filtros. Toque num dia para ver quem está de férias nele. Deslize para trocar de mês.
- **Exportar**: gera um relatório em PDF (A4, retrato) com indicadores, resumo por setor e tabela de cadastros com paginação e rodapé. Dá para exportar tudo ou só um período específico (entram os cadastros que têm algum dia dentro do período), com filtro opcional por setor. Ao gerar, o Android abre a janela de compartilhar para salvar em Arquivos ou Drive, ou enviar por WhatsApp e e-mail.
- **Opções**: backup automático (escolha a pasta do Android e a frequência: 6 horas, 12 horas, todos os dias ou toda semana; mantém as 10 cópias mais recentes e funciona com o app fechado), botão "Fazer backup agora", cópia do banco para compartilhar, importação do banco de dados (substituir tudo ou mesclar, com opção de desfazer) e, no fim da tela, dados do desenvolvedor, versão atual e data da última atualização.
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

O APK é de teste (assinado com a chave de debug que vem no projeto, em `android-overrides/debug.keystore`). Como a chave é sempre a mesma, cada versão nova instala por cima da anterior e os dados continuam no aparelho. Só na primeira vez que você trocar de um APK antigo (feito antes dessa chave) para este é preciso desinstalar o antigo; isso apaga os dados dele, então exporte o PDF ou anote o que precisar antes.

## Backup automático: como funciona

- Ao tocar em **Escolher pasta**, o Android abre o seletor de pastas. Crie uma pasta nova (por exemplo, Documentos > Backup Férias). O Android não deixa escolher a raiz do armazenamento nem a pasta Download.
- Ligue a chave **Backup automático** e escolha a frequência. A primeira cópia é feita na hora.
- O app grava arquivos `ferias-backup-AAAA-MM-DD_HH-mm-ss.json` na pasta. Para restaurar, use **Importar banco de dados** e escolha um desses arquivos.
- A execução em segundo plano é do Android (WorkManager) e pode atrasar com economia de bateria. Se a cópia estiver atrasada, o app faz uma nova assim que você o abre.

## Estrutura

- `www/index.html`: o aplicativo (telas, calendário e regras). `www/vendor/jspdf.umd.min.js` é o gerador de PDF (jsPDF 2.5.1), embutido para funcionar sem internet.
- `android-overrides/res/`: ícone, ícone adaptativo e tela de abertura, copiados para o projeto Android durante o build.
- `capacitor.config.json` e `package.json`: configuração do empacotamento (inclui os plugins Filesystem e Share, usados para entregar o PDF).
- `android-overrides/java/`: código nativo do backup (escolha de pasta, agendamento e gravação), copiado para o projeto Android durante o build.
- `android-overrides/debug.keystore`: chave de assinatura fixa do APK de teste.
- `.github/workflows/build-apk.yml`: receita que compila o APK com Android 13 (API 33) como mínimo.
- `tools/`: arquivos-fonte e script que gerou o ícone e o `www/index.html`. Não são necessários para o build.
