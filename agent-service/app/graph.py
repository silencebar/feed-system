from langgraph.graph import END, START, StateGraph

from app.nodes.creator import format_result, generate_content, review_content, validate_input
from app.state import CreatorState


builder = StateGraph(CreatorState)
builder.add_node("validate_input", validate_input)
builder.add_node("generate_content", generate_content)
builder.add_node("review_content", review_content)
builder.add_node("format_result", format_result)
builder.add_edge(START, "validate_input")
builder.add_edge("validate_input", "generate_content")
builder.add_edge("generate_content", "review_content")
builder.add_edge("review_content", "format_result")
builder.add_edge("format_result", END)

graph = builder.compile()
