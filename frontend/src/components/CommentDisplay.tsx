import { useState, useEffect } from 'react'

/**
 * Comment display component with XSS vulnerability
 * 
 * SEC-04: XSS via dangerouslySetInnerHTML with unsanitized user content
 */
interface CommentDisplayProps {
  theme: any
  config: any
}

interface Comment {
  id: number
  author: string
  content: string
  htmlContent: string
}

function CommentDisplay({ theme, config }: CommentDisplayProps) {
  const [comments, setComments] = useState<Comment[]>([])
  const [newComment, setNewComment] = useState('')
  const [nextId, setNextId] = useState(4) // Start after initial comments
  
  // MNT: Console spam
  console.log('CommentDisplay rendering')
  console.log('Theme:', theme)

  useEffect(() => {
    // Simulated comments that could contain malicious scripts
    setComments([
      {
        id: 1,
        author: 'User1',
        content: 'This is a normal comment',
        htmlContent: '<p>This is a <strong>formatted</strong> comment</p>'
      },
      {
        id: 2,
        author: 'Attacker',
        content: '<script>alert("XSS")</script>',
        htmlContent: '<img src="x" onerror="alert(\'XSS\')">'
      },
      {
        id: 3,
        author: 'Hacker',
        content: 'Check this out!',
        htmlContent: '<a href="javascript:alert(\'XSS\')">Click me</a>'
      }
    ])
  }, [])

  const handleSubmit = () => {
    if (!newComment.trim()) {
      return // Don't add empty comments
    }
    
    // SEC: User input directly used without sanitization
    const newCommentObj: Comment = {
      id: nextId,
      author: 'CurrentUser',
      content: newComment,
      htmlContent: newComment // SEC: Raw user input becomes HTML
    }
    
    setComments([...comments, newCommentObj])
    setNextId(nextId + 1)
    setNewComment('')
    
    console.log('New comment added:', newComment)
  }

  const handleDelete = (id: number) => {
    setComments(comments.filter(comment => comment.id !== id))
  }

  return (
    <div className="card">
      <h2>Comments</h2>
      
      <div className="comments-list">
        {comments.map((comment) => (
          <div key={comment.id} className="comment">
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
              <strong>{comment.author}</strong>
              <button 
                className="button" 
                onClick={() => handleDelete(comment.id)}
                style={{ marginLeft: '10px', padding: '4px 8px', fontSize: '12px' }}
                aria-label={`Delete comment by ${comment.author}`}
              >
                Delete
              </button>
            </div>
            
            {/* SEC-04: XSS - Rendering unsanitized HTML */}
            <div 
              className="comment-content"
              dangerouslySetInnerHTML={{ __html: comment.htmlContent }}
            />
          </div>
        ))}
      </div>
      
      <div className="new-comment">
        <h3>Add Comment (supports HTML!)</h3>
        <textarea
          value={newComment}
          onChange={(e) => setNewComment(e.target.value)}
          placeholder="Enter your comment (HTML allowed)..."
        />
        <button className="button" onClick={handleSubmit}>
          Post Comment
        </button>
      </div>
      
      {/* SEC: Rendering user input without escaping */}
      <div className="preview">
        <h4>Preview:</h4>
        <div dangerouslySetInnerHTML={{ __html: newComment }} />
      </div>
    </div>
  )
}

export default CommentDisplay

