import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Badge, CategoryType } from '../../components/ui/Badge';
import { PostCard, Post } from '../../components/features/posts/PostCard';
import { EmptyFeed } from '../../components/features/posts/EmptyFeed';
import { Navbar } from '../../components/layout/Navbar';
import './PostFeed.css';

// Dummy data matching Figma exactly
const DUMMY_POSTS: Post[] = [
  {
    id: '1',
    title: 'Community Garden Workday This Saturday',
    category: 'Events',
    content: "Join us this Saturday at 9 AM for our monthly community garden workday! We'll be planting spring vegetables and need volunteers. Bring gloves and water. Coffee and donuts provided!",
    author: { id: 'u1', name: 'Sarah Johnson' },
    createdAt: new Date(Date.now() - 2 * 60 * 60 * 1000).toISOString(), // 2 hours ago
    commentCount: 3,
  },
  {
    id: '2',
    title: 'Lost: Orange Tabby Cat',
    category: 'Lost & Found',
    content: "Our cat Whiskers went missing yesterday evening near Oak Street. He's an orange tabby with a white chest, very friendly. Please call 555-0123 if you see him. Reward offered.",
    author: { id: 'u2', name: 'John Smith' },
    createdAt: new Date(Date.now() - 5 * 60 * 60 * 1000).toISOString(), // 5 hours ago
    commentCount: 2,
  },
  {
    id: '3',
    title: 'Best Local Plumber Recommendation?',
    category: 'Recommendations',
    content: "Looking for a reliable plumber to fix a leaky pipe. Does anyone have recommendations for someone trustworthy and reasonably priced in our area?",
    author: { id: 'u3', name: 'Mike Davis' },
    createdAt: new Date(Date.now() - 8 * 60 * 60 * 1000).toISOString(), // 8 hours ago
    commentCount: 5,
  },
  {
    id: '4',
    title: 'Need Help Moving Furniture',
    category: 'Help Requests',
    content: "I'm moving this weekend and could use help moving some heavy furniture up to a second floor apartment. Happy to provide pizza and drinks! Sunday afternoon works best.",
    author: { id: 'u1', name: 'Sarah Johnson' },
    createdAt: new Date(Date.now() - 12 * 60 * 60 * 1000).toISOString(), // 12 hours ago
    commentCount: 8,
  },
  {
    id: '5',
    title: 'Looking for Dog Walker Recommendations',
    category: 'Recommendations',
    content: "Starting a new job and need someone reliable to walk my golden retriever during lunch hours. Any recommendations for dog walkers in the neighborhood?",
    author: { id: 'u4', name: 'Emma Wilson' },
    createdAt: new Date(Date.now() - 24 * 60 * 60 * 1000).toISOString(), // 1 day ago
    commentCount: 6,
  }
];

const CATEGORIES: CategoryType[] = ['All', 'Events', 'Lost & Found', 'Recommendations', 'Help Requests'];

export function PostFeed() {
  const navigate = useNavigate();
  const [searchQuery, setSearchQuery] = useState('');
  const [activeCategory, setActiveCategory] = useState<CategoryType>('All');

  // Filter posts based on category and search query
  const filteredPosts = DUMMY_POSTS.filter((post) => {
    const matchesCategory = activeCategory === 'All' || post.category === activeCategory;
    const matchesSearch = post.title.toLowerCase().includes(searchQuery.toLowerCase()) || 
                          post.content.toLowerCase().includes(searchQuery.toLowerCase());
    return matchesCategory && matchesSearch;
  });

  return (
    <div className="feed-page-container">
      <Navbar />
      
      <main className="feed-main-content">
        
        {/* Top Actions Row: Search & Create */}
        <div className="feed-actions-row">
          <div className="search-and-submit">
            <div className="search-bar-container">
              <img src="/assets/search.svg" alt="Search" className="search-icon-img" />
              <input 
                type="text" 
                placeholder="Search by title of post..." 
                className="search-input"
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
              />
              {searchQuery && (
                <button className="clear-search-btn" onClick={() => setSearchQuery('')}>
                  &times;
                </button>
              )}
            </div>
            
            <button className="search-submit-btn">
              <img src="/assets/search.svg" alt="Search" className="search-submit-icon" />
            </button>
          </div>
          
          <button className="create-post-btn">
            <img src="/assets/plus.svg" alt="Plus" className="plus-icon-img" />
            <span>Create post</span>
          </button>
        </div>

        {/* Categories Row */}
        <div className="categories-row">
          <span className="categories-label">Categories:</span>
          <div className="categories-list">
            {CATEGORIES.map((category) => (
              <Badge 
                key={category}
                category={category}
                isFilter={true}
                isActive={activeCategory === category}
                onClick={() => setActiveCategory(category)}
              />
            ))}
          </div>
        </div>

        {/* Posts List */}
        <div className="posts-list">
          {filteredPosts.length > 0 ? (
            filteredPosts.map((post) => (
              <PostCard 
                key={post.id} 
                post={post} 
                onClick={(id: string) => navigate(`/post/${id}`)} 
              />
            ))
          ) : (
            <EmptyFeed />
          )}
        </div>

        {/* Pagination */}
        <div className="pagination-container">
          <button className="pagination-btn">Previous</button>
          <button className="pagination-btn pagination-active">1</button>
          <button className="pagination-btn">2</button>
          <button className="pagination-btn">3</button>
          <button className="pagination-btn">Next</button>
        </div>
        
      </main>
    </div>
  );
}
