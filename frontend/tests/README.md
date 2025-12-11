# Frontend Testing Guide

This directory contains comprehensive tests for the Papairs frontend application.

## Test Structure

```
tests/
├── unit/                          # Unit tests (isolated component testing)
│   ├── auth.test.js              # Auth utility functions
│   ├── AuthController.test.js    # Authentication controller
│   ├── BaseApiController.test.js # Base API controller
│   └── errorHandler.test.js      # Error handling utilities
├── integration/                   # Integration tests (multi-component workflows)
│   ├── auth-flow.test.js         # Complete authentication flows
│   └── folder-operations.test.js # Folder management workflows
└── __mocks__/                     # Mock files
    └── styleMock.js              # CSS import mocks
```

## Test Types

### Unit Tests
Test individual components in isolation:
- **auth.test.js** (10 tests): Token storage, auth state, user data management
- **AuthController.test.js** (17 tests): Login, logout, register, session management
- **BaseApiController.test.js** (32 tests): HTTP methods, 401 handling, error responses
- **errorHandler.test.js** (16 tests): Safe execution, retry logic, error logging

**Total: 65 unit tests**

### Integration Tests
Test complete workflows with multiple components:
- **auth-flow.test.js** (6 tests): End-to-end authentication scenarios
- **folder-operations.test.js** (8 tests): Complete folder management workflows

**Total: 14 integration tests**

## Running Tests

### Run All Tests
```bash
npm test
```

### Run Unit Tests Only
```bash
npm run test:unit
```

### Run Integration Tests Only
```bash
npm run test:integration
```

### Run Tests in Watch Mode
```bash
npm run test:watch              # All tests
npm run test:unit:watch         # Unit tests only
npm run test:integration:watch  # Integration tests only
```

### Generate Coverage Report
```bash
npm run test:coverage
```

This generates:
- Console coverage summary
- HTML report in `coverage/lcov-report/index.html`

View the HTML report:
```bash
open coverage/lcov-report/index.html
```

## Current Test Coverage

- **Controllers**: 52.02%
  - AuthController: **100%** ✨
  - BaseApiController: **92.85%** ✨
- **Utils**: 30.18%
  - errorHandler: **100%** ✨
  - auth: 46.66%

**Total: 79 tests, all passing** ✅

## What Gets Tested

### ✅ Authentication & Security
- Login/logout flows
- Token storage and validation
- Session management
- 401 error handling and redirects
- Auth header generation

### ✅ API Communication
- All HTTP methods (GET, POST, PUT, PATCH, DELETE)
- Request/response handling
- Error status codes (401, 404, 500, etc.)
- JSON parsing
- URL normalization

### ✅ Folder Operations
- CRUD operations (Create, Read, Update, Delete)
- Folder hierarchy and navigation
- Moving folders
- Folder trees
- Recursive operations

### ✅ Error Handling
- Safe function execution (sync & async)
- Retry logic with exponential backoff
- Error logging and categorization
- Fallback values

## Writing New Tests

### Unit Test Example
```javascript
import { MyComponent } from '@/components/MyComponent'

describe('MyComponent', () => {
  test('should do something', () => {
    const result = MyComponent.doSomething()
    expect(result).toBe('expected value')
  })
})
```

### Integration Test Example
```javascript
describe('Complete Workflow Integration', () => {
  let controller
  
  beforeEach(() => {
    controller = new MyController()
    localStorage.clear()
    global.fetch = jest.fn()
  })
  
  test('should complete full workflow', async () => {
    // Mock API responses
    global.fetch.mockResolvedValue({
      ok: true,
      status: 200,
      headers: { get: () => 'application/json' },
      json: () => Promise.resolve({ data: 'test' })
    })
    
    // Execute workflow
    const result = await controller.performAction()
    
    // Verify results
    expect(result.success).toBe(true)
  })
})
```

## Best Practices

### ✅ Do
- Test critical business logic (auth, API calls, error handling)
- Mock external dependencies (fetch, localStorage, window.location)
- Use descriptive test names
- Clean up after each test (`beforeEach`, `afterEach`)
- Test both success and error cases
- Test edge cases (empty data, null values, invalid JSON)

### ❌ Don't
- Test implementation details
- Test third-party libraries
- Write tests that depend on each other
- Commit coverage reports (`coverage/` is gitignored)
- Skip error cases

## Test Configuration

### jest.config.js
```javascript
{
  testEnvironment: 'jsdom',           // Browser-like environment
  moduleNameMapper: {
    '^@/(.*)$': '<rootDir>/src/$1',   // @ alias support
    '\\.(css|less|scss)$': '<rootDir>/tests/__mocks__/styleMock.js'
  },
  collectCoverageFrom: [
    'src/**/*.{js,vue}',               // All source files
    '!src/main.js',                    // Exclude entry point
    '!src/router/index.js'             // Exclude router config
  ]
}
```

### babel.config.js
Configured to transpile modern JavaScript for Node.js test environment.

## CI/CD Integration

To run tests in CI/CD pipelines:

```bash
# Install dependencies
npm install

# Run tests with coverage
npm run test:coverage

# Exit with error code if tests fail
npm test -- --ci --coverage --maxWorkers=2
```

## Troubleshooting

### Tests timing out
- Increase timeout: `test('name', async () => {...}, 10000)` (10 seconds)
- Check for unresolved promises
- Verify mocks are properly set up

### "Cannot find module '@/...'"
- Ensure `moduleNameMapper` is configured in `jest.config.js`
- Check that the path alias matches your source structure

### "ReferenceError: fetch is not defined"
- Mock fetch in `beforeEach`: `global.fetch = jest.fn()`

### Console errors during tests
- Expected for error testing scenarios
- Use `jest.spyOn(console, 'error').mockImplementation()` to suppress

## Future Test Additions

Consider adding tests for:
- [ ] Document CRUD operations
- [ ] Vue component mounting and interaction
- [ ] Router navigation and guards
- [ ] Vuex/Pinia store actions
- [ ] WebSocket connections
- [ ] File upload/download
- [ ] Form validation

## Resources

- [Jest Documentation](https://jestjs.io/docs/getting-started)
- [Vue Test Utils](https://test-utils.vuejs.org/)
- [Testing Best Practices](https://kentcdodds.com/blog/common-mistakes-with-react-testing-library)
