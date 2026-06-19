# Evidence: Taiga UI instalado e configurado

## Instalação
- `@taiga-ui/cdk` ^5.10.0
- `@taiga-ui/core` ^5.10.0
- `@taiga-ui/kit` ^5.10.0
- `@taiga-ui/styles` ^5.10.0
- `@taiga-ui/design-tokens` ^0.303.0
- `@taiga-ui/event-plugins` ^5.0.0
- `@taiga-ui/i18n` ^5.10.0
- `@taiga-ui/polymorpheus` ^5.0.1
- `@ng-web-apis/common` ^5.3.0
- `@ng-web-apis/intersection-observer` ^5.3.0
- `@ng-web-apis/mutation-observer` ^5.3.0
- `@ng-web-apis/platform` ^5.3.0
- `@angular/cdk` ^22.0.0
- `@angular/animations` ^22.0.0
- `less` (dev)

## Configurações aplicadas
1. `angular.json` — estilos globais `@taiga-ui/styles/taiga-ui-theme.less` e `taiga-ui-fonts.less`
2. `app.config.ts` — `provideAnimations()` adicionado
3. `app.ts` — `TuiRoot` importado e adicionado ao `imports`
4. `app.html` — `<tui-root>` wrapper adicionado
5. `app.spec.ts` — providers `provideAnimations()` + `TUI_OPTIONS` adicionados

## Resultados
- Build: ✅ concluído (393 kB main, 69 kB styles)
- Testes: ✅ 26/26 passando (4 arquivos, todos verdes)
- `grep "@taiga-ui" package.json` — 9 linhas
- `grep "taiga-ui-theme" angular.json` — linha presente
- `grep "TuiRoot" src/app/app.ts` — linha presente
- `grep "tui-root" src/app/app.html` — tag presente
