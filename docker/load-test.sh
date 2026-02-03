#!/bin/bash

# FAANG-Level Load Testing Script
# Demonstrates high-throughput event processing capabilities

echo "🚀 Starting FAANG-Level Load Test..."

# Configuration
APP_URL="http://localhost:8080"
CONCURRENT_USERS=50
EVENTS_PER_USER=1000
EVENT_TYPES=("user_signup" "purchase" "page_view" "user_login" "cart_add")

# Function to generate random user ID
generate_user_id() {
    echo "user_$(shuf -i 1000-9999 -n 1)"
}

# Function to generate random event type
generate_event_type() {
    echo "${EVENT_TYPES[$RANDOM % ${#EVENT_TYPES[@]}]}"
}

# Function to send events
send_events() {
    local user_num=$1
    local events_count=$2
    
    echo "User $user_num: Sending $events_count events..."
    
    for ((i=1; i<=events_count; i++)); do
        user_id=$(generate_user_id)
        event_type=$(generate_event_type)
        
        # Send event with payload
        curl -s -X POST "$APP_URL/api/events" \
            -H "Content-Type: application/json" \
            -d "{\"amount\": \"$((RANDOM % 1000))\", \"product_id\": \"prod_$((RANDOM % 100))\"}" \
            --data-urlencode "eventType=$event_type" \
            --data-urlencode "userId=$user_id" > /dev/null
        
        # Throttle to avoid overwhelming
        if ((i % 100 == 0)); then
            echo "User $user_num: Sent $i events"
            sleep 0.1
        fi
    done
    
    echo "✅ User $user_num: Completed $events_count events"
}

# Start load test
echo "📊 Load Test Configuration:"
echo "   - Concurrent Users: $CONCURRENT_USERS"
echo "   - Events per User: $EVENTS_PER_USER"
echo "   - Total Events: $((CONCURRENT_USERS * EVENTS_PER_USER))"
echo "   - Target Throughput: ~10,000 events/sec"
echo ""

start_time=$(date +%s)

# Launch concurrent users
for ((user=1; user<=CONCURRENT_USERS; user++)); do
    send_events $user $EVENTS_PER_USER &
done

# Wait for all background jobs to complete
wait

end_time=$(date +%s)
duration=$((end_time - start_time))
total_events=$((CONCURRENT_USERS * EVENTS_PER_USER))
throughput=$((total_events / duration))

echo ""
echo "🎯 Load Test Results:"
echo "   - Total Events: $total_events"
echo "   - Duration: ${duration}s"
echo "   - Throughput: ${throughput} events/sec"
echo ""

# Get real-time metrics
echo "📈 Real-time Metrics:"
curl -s "$APP_URL/api/metrics/realtime" | jq '.'

echo ""
echo "✨ Load test completed! Check Grafana dashboard for detailed metrics."
