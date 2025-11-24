/**
 * Validation constants for form fields
 * These limits ensure realistic data entry and prevent database issues
 */

export const VALIDATION_LIMITS = {
  // Personal information
  NAME: 100, // First name, last name, full name
  TITLE: 100, // Job title, position
  PHONE: 20, // Phone numbers
  EMAIL: 255, // Email addresses (standard max)
  
  // Business information
  COMPANY_NAME: 200, // Company/account name
  DOMAIN: 255, // Website domain
  INDUSTRY: 100, // Industry type
  
  // Subject/Title fields
  SUBJECT: 200, // Ticket subject, deal name, lead name
  TICKET_SUBJECT: 200, // Support ticket subject
  
  // Description fields
  DESCRIPTION: 5000, // Long descriptions, ticket details
  NOTES: 5000, // Notes, comments
  TICKET_DESCRIPTION: 5000, // Support ticket description
  
  // Chat/Messaging
  CHAT_MESSAGE: 2000, // Chat messages, inbox messages
  SUPPORT_AI_MESSAGE: 2000, // Support AI chat messages
  EMAIL_BODY: 10000, // Email body content
  EMAIL_SUBJECT: 200, // Email subject line
  
  // Search/Filter
  SEARCH_QUERY: 200, // Search inputs
  
  // Address fields
  ADDRESS: 500, // Street address
  CITY: 100, // City name
  STATE: 100, // State/province
  ZIP_CODE: 20, // Postal code
  COUNTRY: 100, // Country name
  
  // Other
  SOURCE: 100, // Lead source, deal source
  STATUS: 50, // Status values
  PRIORITY: 20, // Priority levels
  CATEGORY: 100, // Categories
  TAG: 50, // Tags
  CURRENCY: 10, // Currency codes
  URL: 2048, // URLs (standard max)
} as const;

/**
 * Get validation limit for a field type
 */
export function getMaxLength(fieldType: keyof typeof VALIDATION_LIMITS): number {
  return VALIDATION_LIMITS[fieldType];
}

/**
 * Validate field value against limit
 */
export function validateLength(
  value: string,
  fieldType: keyof typeof VALIDATION_LIMITS
): { valid: boolean; remaining: number } {
  const maxLength = VALIDATION_LIMITS[fieldType];
  const length = value.length;
  return {
    valid: length <= maxLength,
    remaining: Math.max(0, maxLength - length),
  };
}

