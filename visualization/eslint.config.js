// @ts-check
const eslint = require('@eslint/js');
const tseslint = require('typescript-eslint');
const angular = require('angular-eslint');
require("eslint-plugin-only-warn");

module.exports = tseslint.config(
  {
    files: ['**/*.ts'],
    extends: [
      eslint.configs.recommended,
      ...tseslint.configs.recommended,
      ...tseslint.configs.stylistic,
      ...angular.configs.tsRecommended,
    ],
    processor: angular.processInlineTemplates,
    rules: {
      '@typescript-eslint/no-inferrable-types': 'off',
      '@typescript-eslint/no-namespace': 'warn',
      '@typescript-eslint/no-explicit-any': 'warn',
      'no-unused-vars': 'warn',
      '@typescript-eslint/no-unused-vars': [
        'error',
        {
          args: 'none',
          caughtErrors: 'none',
          vars: 'all',
          varsIgnorePattern: '^_$',
          destructuredArrayIgnorePattern: '^_$',
        },
      ],
      '@angular-eslint/directive-selector': [
        'error',
        {
          type: 'attribute',
          style: 'camelCase',
        },
      ],
      '@angular-eslint/component-selector': [
        'error',
        {
          type: 'element',
          style: 'kebab-case',
        },
      ],
    },
  },
  {
    files: ['**/*.html'],
    extends: [
      ...angular.configs.templateRecommended,
      ...angular.configs.templateAccessibility,
    ],
    rules: {},
  },
);
