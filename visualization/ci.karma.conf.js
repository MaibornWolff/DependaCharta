// Karma configuration file, see link for more information
// https://karma-runner.github.io/1.0/config/configuration-file.html

module.exports = function (config) {
  config.set({
    frameworks: ['jasmine', '@angular-devkit/build-angular'],
    plugins: [
      require('karma-jasmine'),
      require('karma-chrome-launcher'),
      require('karma-jasmine-html-reporter'),
      require('karma-coverage'),
      require('@angular-devkit/build-angular/plugins/karma'),
      require('karma-junit-reporter')
    ],
    reporters: ['progress', 'junit', 'coverage'],
    browsers: ['ChromeHeadlessNoSandbox'],
    customLaunchers: {
      ChromeHeadlessNoSandbox: {
        base: 'ChromeHeadless',
        flags: ['--no-sandbox', '--disable-gpu']
      }
    },
    singleRun: true,
    preprocessors: {
      'src/**/*.ts': ['coverage']
    },
    coverageReporter: {
      dir: 'coverage',
      reporters: [
        { type: 'lcovonly', subdir: '.', file: 'lcov.info' },
      ]
    },
    junitReporter: {
      outputDir: 'test-results'
    }
  });
};
