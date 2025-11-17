/**
 * Content formatter utility for converting between HTML and ProseMirror formats
 * Simplified version that focuses on HTML processing
 */

class ContentFormatter {
  /**
   * Extract content from Hocuspocus Y.js document for saving
   * Uses direct Y.js access for reliable extraction
   * @param {Y.Doc} ydoc - The Y.js document
   * @returns {Object} - Extracted content and metadata
   */
  static extractFromYDoc(ydoc) {
    console.log('🔍 ContentFormatter: Starting content extraction for save operation')
    
    const result = {
      content: '',
      extractionMethod: 'none',
      success: false,
      metadata: {
        yDocKeys: ydoc.share ? Array.from(ydoc.share.keys()) : [],
        yDocSize: ydoc.share ? ydoc.share.size : 0
      }
    }

    console.log('🔍 Y.Doc metadata:', result.metadata)

    // Method 1: Try to extract from ProseMirror XML fragment (most reliable)
    try {
      const fragment = ydoc.getXmlFragment('default')
      
      if (fragment) {
        console.log('✅ Found ProseMirror XML fragment')
        
        // Get the XML string representation
        let xmlString = ''
        
        // Try different ways to get the content
        if (typeof fragment.toString === 'function') {
          xmlString = fragment.toString()
        } else if (fragment.toJSON) {
          const jsonData = fragment.toJSON()
          xmlString = JSON.stringify(jsonData)
        } else if (fragment._start && fragment._start.content) {
          // Try to access internal content structure
          xmlString = this.extractFromFragmentContent(fragment)
        }
        
        console.log('📄 Raw XML:', xmlString)
        
        if (xmlString && xmlString.length > 0) {
          result.content = this.convertXmlToHtml(xmlString)
          result.extractionMethod = 'prosemirror-xml'
          result.success = true
          
          console.log('✅ Content extracted via ProseMirror XML:', {
            xmlLength: xmlString.length,
            htmlLength: result.content.length,
            preview: result.content.substring(0, 100)
          })
          
          return result
        }
      }
    } catch (error) {
      console.log('❌ ProseMirror XML extraction failed:', error.message)
    }

    // Method 2: Try direct Y.js state extraction
    try {
      if (ydoc.share && ydoc.share.size > 0) {
        for (const [key, sharedType] of ydoc.share.entries()) {
          console.log(`🔍 Checking shared type: ${key}`)
          
          if (sharedType && typeof sharedType.toJSON === 'function') {
            const jsonContent = sharedType.toJSON()
            console.log('📄 JSON content:', jsonContent)
            
            if (jsonContent && (typeof jsonContent === 'string' || typeof jsonContent === 'object')) {
              let content = typeof jsonContent === 'string' ? jsonContent : JSON.stringify(jsonContent)
              
              if (content && content.length > 0) {
                result.content = content.includes('<') ? content : `<p>${content}</p>`
                result.extractionMethod = 'yjs-json'
                result.success = true
                
                console.log('✅ Content extracted via Y.js JSON')
                return result
              }
            }
          }
        }
      }
    } catch (error) {
      console.log('❌ Y.js JSON extraction failed:', error.message)
    }

    // Method 3: Fallback to empty content (better than failing completely)
    console.log('⚠️ No content found, returning empty content for save')
    result.content = '<p></p>'
    result.extractionMethod = 'empty-fallback'
    result.success = true
    
    return result
  }

  /**
   * Extract content from fragment internal structure
   * @param {Object} fragment - Y.js XML fragment
   * @returns {string} - Extracted content
   */
  static extractFromFragmentContent(fragment) {
    try {
      // This is a fallback method for complex Y.js structures
      if (fragment._start && fragment._start.content) {
        return fragment._start.content.toString()
      }
      return ''
    } catch (error) {
      console.log('❌ Fragment content extraction failed:', error.message)
      return ''
    }
  }

  /**
   * Convert ProseMirror XML to HTML format
   * @param {string} xmlString - The ProseMirror XML string
   * @returns {string} - Formatted HTML content
   */
  static convertXmlToHtml(xmlString) {
    console.log('🔄 Converting ProseMirror XML to HTML')
    console.log('📄 Raw XML input:', xmlString)
    
    let html = xmlString
    
    // Convert paragraph structure
    html = html.replace(/<paragraph>/g, '<p>')
    html = html.replace(/<\/paragraph>/g, '</p>')
    
    // Convert headings with level attributes
    html = html.replace(/<heading level="(\d+)">/g, '<h$1>')
    html = html.replace(/<\/heading>/g, '</h1>') // Will be fixed by the next replacement
    html = html.replace(/<\/h1>/g, (match, offset, string) => {
      // Find the most recent h tag before this position
      const beforeText = string.substring(0, offset)
      const levelMatch = beforeText.match(/.*<h(\d+)>(?!.*<h\d+>)/s)
      return levelMatch ? `</h${levelMatch[1]}>` : '</h1>'
    })
    
    // Handle text formatting - PRESERVE all existing formatting
    // ProseMirror uses different tag names than standard HTML
    
    // Bold formatting - ProseMirror uses <bold>, not <strong>
    html = html.replace(/<bold>/g, '<strong>')
    html = html.replace(/<\/bold>/g, '</strong>')
    html = html.replace(/<strong>/g, '<strong>')  // Keep existing strong tags
    html = html.replace(/<\/strong>/g, '</strong>')
    
    // Italic formatting - ProseMirror uses <italic>, not <em>
    html = html.replace(/<italic>/g, '<em>')
    html = html.replace(/<\/italic>/g, '</em>')
    html = html.replace(/<em>/g, '<em>')  // Keep existing em tags
    html = html.replace(/<\/em>/g, '</em>')
    
    // Code formatting
    html = html.replace(/<code>/g, '<code>')
    html = html.replace(/<\/code>/g, '</code>')
    
    // Underline (if present)
    html = html.replace(/<underline>/g, '<u>')
    html = html.replace(/<\/underline>/g, '</u>')
    html = html.replace(/<u>/g, '<u>')
    html = html.replace(/<\/u>/g, '</u>')
    
    // Strike through - ProseMirror might use different tags
    html = html.replace(/<strike>/g, '<s>')
    html = html.replace(/<\/strike>/g, '</s>')
    html = html.replace(/<s>/g, '<s>')
    html = html.replace(/<\/s>/g, '</s>')
    
    // Subscript and Superscript
    html = html.replace(/<sub>/g, '<sub>')
    html = html.replace(/<\/sub>/g, '</sub>')
    html = html.replace(/<sup>/g, '<sup>')
    html = html.replace(/<\/sup>/g, '</sup>')
    
    // Text color and highlighting - preserve style attributes
    html = html.replace(/<span([^>]*)>/g, '<span$1>')
    html = html.replace(/<\/span>/g, '</span>')
    
    // Handle mark elements with style attributes (for colors, highlights, etc.)
    html = html.replace(/<mark([^>]*)>/g, '<mark$1>')
    html = html.replace(/<\/mark>/g, '</mark>')
    
    // Line breaks
    html = html.replace(/<hardbreak><\/hardbreak>/g, '<br>')
    html = html.replace(/<hardbreak\/>/g, '<br>')
    html = html.replace(/<br><\/br>/g, '<br>')
    
    // Lists - preserve all list formatting
    html = html.replace(/<bulletList>/g, '<ul>')
    html = html.replace(/<\/bulletList>/g, '</ul>')
    html = html.replace(/<orderedList>/g, '<ol>')
    html = html.replace(/<\/orderedList>/g, '</ol>')
    html = html.replace(/<listItem>/g, '<li>')
    html = html.replace(/<\/listItem>/g, '</li>')
    
    // Blockquotes
    html = html.replace(/<blockquote>/g, '<blockquote>')
    html = html.replace(/<\/blockquote>/g, '</blockquote>')
    
    // Horizontal rules
    html = html.replace(/<horizontalRule><\/horizontalRule>/g, '<hr>')
    html = html.replace(/<horizontalRule\/>/g, '<hr>')
    
    // Links - preserve href attributes
    html = html.replace(/<link([^>]*)>/g, '<a$1>')
    html = html.replace(/<\/link>/g, '</a>')
    
    // Text alignment - convert to CSS style
    html = html.replace(/<p([^>]*) textAlign="([^"]*)"([^>]*)>/g, '<p$1 style="text-align: $2"$3>')
    html = html.replace(/<h([1-6])([^>]*) textAlign="([^"]*)"([^>]*)>/g, '<h$1$2 style="text-align: $3"$4>')
    
    // Clean up any remaining ProseMirror-specific XML tags but KEEP their content
    // Only remove tags we don't recognize, but preserve content and known HTML attributes
    const preservedAttributes = ['style', 'class', 'href', 'src', 'alt', 'title', 'target']
    
    // Handle text nodes with marks (preserve all formatting)
    html = html.replace(/<text>/g, '')
    html = html.replace(/<\/text>/g, '')
    
    // Remove unknown XML tags but keep content and attributes for known HTML elements
    html = html.replace(/<(\/?)([\w-]+)([^>]*)>/g, (match, slash, tagName, attributes) => {
      const knownHtmlTags = [
        'p', 'h1', 'h2', 'h3', 'h4', 'h5', 'h6', 
        'strong', 'em', 'u', 's', 'code', 'sub', 'sup',
        'span', 'mark', 'br', 'hr',
        'ul', 'ol', 'li', 
        'blockquote', 'a', 'img',
        'table', 'thead', 'tbody', 'tr', 'td', 'th'
      ]
      
      if (knownHtmlTags.includes(tagName.toLowerCase())) {
        return `<${slash}${tagName}${attributes}>`
      }
      
      // For unknown tags, just remove the tag but keep the content
      return ''
    })
    
    // Clean up extra whitespace but preserve intentional formatting
    html = html
      .replace(/\n\s*\n/g, '\n') // Remove multiple empty lines
      .replace(/>\s+</g, '><') // Remove spaces between tags
      .trim()
    
    console.log('✅ XML to HTML conversion complete:', {
      originalLength: xmlString.length,
      htmlLength: html.length,
      preview: html.substring(0, 300),
      fullResult: html
    })
    
    return html
  }

  /**
   * Convert HTML back to ProseMirror XML format (for loading)
   * @param {string} html - The HTML content
   * @returns {string} - ProseMirror XML format
   */
  static convertHtmlToXml(html) {
    console.log('🔄 Converting HTML to ProseMirror XML')
    
    if (!html || html.trim() === '') {
      return ''
    }
    
    let xml = html
    
    // Convert HTML paragraphs to ProseMirror paragraphs
    xml = xml.replace(/<p>/g, '<paragraph>')
    xml = xml.replace(/<\/p>/g, '</paragraph>')
    
    // Convert headings
    xml = xml.replace(/<h(\d+)>/g, '<heading level="$1">')
    xml = xml.replace(/<\/h\d+>/g, '</heading>')
    
    // Convert line breaks
    xml = xml.replace(/<br\s*\/?>/g, '<hardbreak></hardbreak>')
    
    // Convert lists
    xml = xml.replace(/<ul>/g, '<bulletList>')
    xml = xml.replace(/<\/ul>/g, '</bulletList>')
    xml = xml.replace(/<ol>/g, '<orderedList>')
    xml = xml.replace(/<\/ol>/g, '</orderedList>')
    xml = xml.replace(/<li>/g, '<listItem>')
    xml = xml.replace(/<\/li>/g, '</listItem>')
    
    // Convert blockquotes
    xml = xml.replace(/<blockquote>/g, '<blockquote>')
    xml = xml.replace(/<\/blockquote>/g, '</blockquote>')
    
    // Convert horizontal rules
    xml = xml.replace(/<hr\s*\/?>/g, '<horizontalRule></horizontalRule>')
    
    // Text formatting stays the same (strong, em, code)
    
    console.log('✅ HTML to XML conversion complete:', {
      originalLength: html.length,
      xmlLength: xml.length
    })
    
    return xml
  }

  /**
   * Sanitize and validate content
   * @param {string} content - Content to sanitize
   * @returns {string} - Sanitized content
   */
  static sanitizeContent(content) {
    if (typeof content !== 'string') {
      return ''
    }
    
    // Basic HTML sanitization (remove scripts, dangerous attributes)
    return content
      .replace(/<script[^>]*>.*?<\/script>/gi, '')
      .replace(/<iframe[^>]*>.*?<\/iframe>/gi, '')
      .replace(/on\w+="[^"]*"/gi, '')
      .replace(/javascript:/gi, '')
      .trim()
  }
}

module.exports = ContentFormatter