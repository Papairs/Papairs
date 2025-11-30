/**
 * Base API Controller class providing common functionality for HTTP requests
 */
export class BaseApiController {
  constructor(baseURL) {
    this.baseURL = baseURL;
    this.defaultHeaders = {
      'Content-Type': 'application/json'
    };
  }

  /**
   * Get authorization headers if token exists
   */
  getAuthHeaders() {
    const token = localStorage.getItem('papairs_token');
    return token ? { 'Authorization': `Bearer ${token}` } : {};
  }

  /**
   * Get user ID headers
   */
  getUserHeaders() {
    try {
      const userData = localStorage.getItem('papairs_user');
      const user = userData ? JSON.parse(userData) : null;
      const userId = user?.id;
      return userId ? { 'X-User-Id': userId } : {};
    } catch (error) {
      console.error('Error parsing user data from localStorage:', error);
      return {};
    }
  }

  /**
   * Get combined headers
   */
  getHeaders(additionalHeaders = {}) {
    return {
      ...this.defaultHeaders,
      ...this.getAuthHeaders(),
      ...this.getUserHeaders(),
      ...additionalHeaders
    };
  }

  /**
   * Generic HTTP request method
   * Removes trailing slashes from baseURL and ensures single slash between baseURL and endpoint
   */
  async makeRequest(method, endpoint, data = null, additionalHeaders = {}) {
    const cleanBase = this.baseURL.replace(/\/$/, '');
    const cleanEndpoint = endpoint.startsWith('/') ? endpoint : `/${endpoint}`;
    const url = `${cleanBase}${cleanEndpoint}`;
    const headers = this.getHeaders(additionalHeaders);

    const config = {
      method,
      headers,
    };

    if (data && (method === 'POST' || method === 'PUT' || method === 'PATCH')) {
      config.body = JSON.stringify(data);
    }

    try {
      const response = await fetch(url, config);
      const result = await this.handleResponse(response);
      return {
        success: true,
        data: result,
        status: response.status
      };
    } catch (error) {
      return {
        success: false,
        error: error.message || 'Request failed',
        status: error.status || 500
      };
    }
  }

  /**
   * Handle response and parse JSON
   */
  async handleResponse(response) {
    const contentType = response.headers.get('content-type');
    
    if (!response.ok) {
      let errorMessage = `HTTP ${response.status}`;
      
      if (contentType && contentType.includes('application/json')) {
        try {
          const errorData = await response.json();
          errorMessage = errorData.message || errorData.error || errorMessage;
        } catch {
          // Fallback to status text if JSON parsing fails
          errorMessage = response.statusText || errorMessage;
        }
      } else {
        errorMessage = response.statusText || errorMessage;
      }
      
      const error = new Error(errorMessage);
      error.status = response.status;
      throw error;
    }

    if (response.status === 204) {
      return null; // No content
    }

    if (contentType && contentType.includes('application/json')) {
      return await response.json();
    }

    return await response.text();
  }

  // HTTP method shortcuts
  async get(endpoint, additionalHeaders = {}) {
    return this.makeRequest('GET', endpoint, null, additionalHeaders);
  }

  async post(endpoint, data = null, additionalHeaders = {}) {
    return this.makeRequest('POST', endpoint, data, additionalHeaders);
  }

  async put(endpoint, data = null, additionalHeaders = {}) {
    return this.makeRequest('PUT', endpoint, data, additionalHeaders);
  }

  async patch(endpoint, data = null, additionalHeaders = {}) {
    return this.makeRequest('PATCH', endpoint, data, additionalHeaders);
  }

  async delete(endpoint, additionalHeaders = {}) {
    return this.makeRequest('DELETE', endpoint, null, additionalHeaders);
  }
}