# Singleflight Pattern là gì?

**Singleflight pattern** là một thiết kế mẫu xử lý bất đồng bộ (concurrency design pattern). Pattern này được dùng để ngăn chặn việc lặp lại các tác vụ xử lý trùng lặp khi có nhiều yêu cầu (requests) đồng thời cùng đòi hỏi một dữ liệu giống nhau.

Thay vì thực hiện cùng một thao tác xử lý tốn kém tài nguyên nhiều lần, yêu cầu đầu tiên sẽ thực hiện tác vụ đó, trong khi các yêu cầu đồng thời còn lại sẽ đợi và dùng chung chính kết quả thu được.

---

## Tại sao nên sử dụng?

* **Ngăn chặn Cache Stampede (Thundering Herd):** Khi một bản ghi cache phổ biến bị hết hạn, hàng ngàn request đồng thời có thể đổ bộ vào hệ thống cùng lúc, vượt qua tầng cache và làm quá tải cơ sở dữ liệu. Singleflight giới hạn các truy vấn này về đúng một cuộc gọi cơ sở dữ liệu duy nhất.
* **Giảm thiểu tiêu tốn tài nguyên:** Tiết kiệm năng lượng tính toán, bộ nhớ và băng thông mạng bằng cách gom nhóm các tác vụ trùng lặp đang chạy (in-flight operations).
* **Bảo vệ API của bên thứ ba:** Giới hạn số lượng request giúp bạn không bị vượt quá hạn mức sử dụng (quota) hoặc bị chặn do giới hạn tần suất (rate limits) từ các dịch vụ bên ngoài.

---

## Cách thức hoạt động

Pattern này hoạt động thông qua một đối tượng quản lý gọi là `Group` (đóng vai trò như một không gian tên cho các đơn vị tác vụ). Quá trình diễn ra qua 3 bước đơn giản:

1. **Gán khóa (Key):** Các request cho cùng một tài nguyên (ví dụ: một ID người dùng hoặc một URL cụ thể) được ánh xạ vào một khóa duy nhất.
2. **Ai đến trước phục vụ trước:** Khi request đầu tiên cho một khóa xuất hiện, hàm xử lý thực tế sẽ được kích hoạt.
3. **Chờ và chia sẻ:** Nếu các request tiếp theo cho *cùng khóa đó* đến trong lúc request đầu tiên vẫn đang xử lý, chúng sẽ được tạm dừng. Ngay khi request đầu hoàn thành, kết quả của nó sẽ lập tức được chia sẻ cho tất cả các request đang đợi.

---

## Các triển khai phổ biến

Mặc dù pattern này có thể tự viết bằng bất kỳ ngôn ngữ nào (như Java hay Rust), nó nổi tiếng nhất dưới dạng một thư viện mở rộng chính thức về concurrency trong ngôn ngữ Go (Golang): `golang.org/x/sync/singleflight`.

Các phương thức chính của nó thường bao gồm:
* `Do(key, func)`: Chạy hàm xử lý, đồng thời chặn các request trùng lặp để đợi kết quả.
* `DoChan(key, func)`: Chạy hàm xử lý nhưng trả về kết quả qua một kênh (channel) thay vì chặn luồng (blocking thread).

---

## Hạn chế cần lưu ý

* **Không lưu trữ cache lâu dài:** Ngay khi "chuyến bay" (flight) của các request đồng thời kết thúc, kết quả sẽ bị xóa bỏ. Nó không thay thế được các tầng cache lưu trữ lâu dài như Redis, vì các request đến muộn hơn sau đó vẫn sẽ kích hoạt lại tác vụ từ đầu.
* **Đánh đổi về độ trễ (Latency):** Các request sau buộc phải đợi tiến trình đầu tiên hoàn thành. Nếu tiến trình đầu bị chậm, tất cả các request đợi cùng nó cũng sẽ bị chậm theo.
