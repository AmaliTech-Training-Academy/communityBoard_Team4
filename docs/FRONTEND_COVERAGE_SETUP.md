# Frontend Test Coverage Configuration

## Update package.json

Add these scripts to `frontend/package.json`:

```json
{
  "scripts": {
    "test": "react-scripts test",
    "test:coverage": "react-scripts test --coverage --watchAll=false",
    "lint": "eslint src --ext .js,.jsx --max-warnings 0",
    "lint:fix": "eslint src --ext .js,.jsx --fix"
  },
  "jest": {
    "collectCoverageFrom": [
      "src/**/*.{js,jsx}",
      "!src/index.js",
      "!src/reportWebVitals.js",
      "!src/**/*.test.{js,jsx}"
    ],
    "coverageThreshold": {
      "global": {
        "branches": 70,
        "functions": 70,
        "lines": 70,
        "statements": 70
      }
    }
  }
}
```

## Install ESLint

```bash
cd frontend
npm install --save-dev eslint eslint-config-react-app
```

## Create .eslintrc.json

```json
{
  "extends": ["react-app"],
  "rules": {
    "no-console": "warn",
    "no-debugger": "error",
    "no-unused-vars": "warn"
  }
}
```

## Create .eslintignore

```
build/
node_modules/
coverage/
public/
*.config.js
```
